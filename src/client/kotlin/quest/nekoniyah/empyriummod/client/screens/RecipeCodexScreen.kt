package quest.nekoniyah.empyriummod.client.screens

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import quest.nekoniyah.empyriummod.client.EmpyriumModClient
import quest.nekoniyah.empyriummod.client.util.ui.Helpers.column
import quest.nekoniyah.empyriummod.client.util.ui.Helpers.label
import quest.nekoniyah.empyriummod.network.packets.RecipeCodexPacketEntry
import net.minecraft.network.chat.Component as Text

class RecipeCodexScreen : EmpyriumScreen("Recipe Codex") {
    override val identifier: String = "recipe_codex"

    private var hideCompleted = false
    private var search = ""
    private var page = 0
    private var pager: FlowLayout? = null

    private var summary: FlowLayout? = null
    private var list: FlowLayout? = null

    override fun build(rootComponent: FlowLayout) {
        rootComponent.surface(Surface.flat(0x77000000)).verticalAlignment(VerticalAlignment.CENTER)
            .horizontalAlignment(HorizontalAlignment.CENTER).sizing(Sizing.fill())

        val panel = Containers.verticalFlow(Sizing.fixed(WIDTH + 16), Sizing.content())
        panel.surface(Surface.DARK_PANEL).padding(Insets.of(8))
        panel.horizontalAlignment(HorizontalAlignment.CENTER)
        panel.gap(5)

        panel.child(label("Recipe Codex"))

        val newSummary = column()
        summary = newSummary
        panel.child(newSummary)

        val searchBox = Components.textBox(Sizing.fixed(WIDTH - 10), search)
        searchBox.setMaxLength(50)
        searchBox.setHint(Text.literal("Search name or item ID..."))
        searchBox.onChanged().subscribe { value ->
            search = value
            page = 0
            refresh()
        }
        panel.child(searchBox)

        panel.child(Components.button(filterText()) { button ->
            hideCompleted = !hideCompleted
            page = 0
            button.message = filterText()
            refresh()
        })

        val newList = column()
        list = newList
        panel.child(
            Containers.verticalScroll(Sizing.fixed(WIDTH), Sizing.fixed(maxOf(60, height - 150)), newList)
        )

        val newPager = Containers.horizontalFlow(Sizing.content(), Sizing.content())
        newPager.verticalAlignment(VerticalAlignment.CENTER)
        newPager.gap(6)
        pager = newPager
        panel.child(newPager)

        rootComponent.child(panel)
        refresh()
    }

    fun refresh() {
        val summary = summary ?: return
        val list = list ?: return
        val pager = pager ?: return
        val entries = EmpyriumModClient.codex

        // Overall progress
        val tracked = entries.filter { it.goal > 0 }
        val done = tracked.count { isComplete(it) }
        summary.clearChildren()
        summary.child(label("$done / ${tracked.size} goals completed", GRAY))
        summary.child(progressBar(WIDTH - 20, done, tracked.size, done == tracked.size && done > 0))

        // Unfinished first, closest to completion at the top
        val visible = entries.filterNot { hideCompleted && isComplete(it) }.filter { matchesSearch(it) }
            .sortedWith(compareBy<RecipeCodexPacketEntry> { it.goal <= 0 }.thenBy { isComplete(it) }
                .thenByDescending { fraction(it.current, it.goal) }.thenBy { it.itemKey })

        list.clearChildren()

        if (visible.isEmpty()) {
            val message = when {
                entries.isEmpty() -> "No recipes to show."
                search.isNotBlank() -> "No matching recipes."
                else -> "All recipes completed!"
            }
            list.child(label(message, GRAY))
        }

        val pages = maxOf(1, (visible.size + PAGE_SIZE - 1) / PAGE_SIZE)
        page = page.coerceIn(0, pages - 1)
        visible.drop(page * PAGE_SIZE).take(PAGE_SIZE).forEach { list.child(entryRow(it)) }

        pager.clearChildren()
        pager.child(pageButton("<", page > 0, -1))
        pager.child(label("Page ${page + 1} / $pages"))
        pager.child(pageButton(">", page < pages - 1, 1))
    }

    private fun pageButton(text: String, enabled: Boolean, step: Int) = Components.button(Text.literal(text)) {
        page += step
        refresh()
    }.also { it.active = enabled }

    private fun matchesSearch(entry: RecipeCodexPacketEntry): Boolean {
        val query = search.trim()
        return entry.itemKey.contains(query, ignoreCase = true) || itemFor(entry.itemKey)?.hoverName?.string?.contains(
            query, ignoreCase = true
        ) == true
    }

    private fun entryRow(entry: RecipeCodexPacketEntry): FlowLayout {
        val stack = itemFor(entry.itemKey)
        val name = stack?.hoverName?.string ?: entry.itemKey
        val complete = isComplete(entry)

        val row = Containers.horizontalFlow(Sizing.fixed(WIDTH - 10), Sizing.content())
        row.surface(Surface.flat(0x55000000))
        row.padding(Insets.of(4))
        row.verticalAlignment(VerticalAlignment.CENTER)
        row.gap(6)

        val icon = Components.item(stack ?: ItemStack(Items.BARRIER))
        if (stack != null) icon.setTooltipFromStack(true)
        row.child(icon)

        val info = Containers.verticalFlow(Sizing.fixed(TEXT_WIDTH), Sizing.content())
        info.gap(2)

        info.child(label(name).maxWidth(TEXT_WIDTH))

        val progress = if (entry.goal <= 0) "Crafted: ${entry.current} (no goal)"
        else if (complete) "Completed (${entry.current} / ${entry.goal})"
        else "${entry.current} / ${entry.goal} (${(fraction(entry.current, entry.goal) * 100).toInt()}%)"
        info.child(label(progress, if (complete) GREEN else GRAY))

        if (entry.goal > 0) info.child(progressBar(TEXT_WIDTH, entry.current, entry.goal, complete))

        if (entry.totalFreeCount > 0) {
            val left = (entry.totalFreeCount - entry.usedFreeCount).coerceAtLeast(0)
            info.child(label("Free: $left / ${entry.totalFreeCount} left", if (left > 0) GOLD else GRAY))
        }

        row.child(info)
        return row
    }

    private fun progressBar(width: Int, current: Int, goal: Int, complete: Boolean): FlowLayout {
        val track = Containers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(BAR_HEIGHT))
        track.surface(Surface.flat(0xFF333333.toInt()))

        val filled = (width * fraction(current, goal)).toInt()
        if (filled > 0) {
            val fill = Containers.horizontalFlow(Sizing.fixed(filled), Sizing.fixed(BAR_HEIGHT))
            val color = if (complete) 0xFF55FF55 else 0xFF55AAFF
            fill.surface(Surface.flat(color.toInt()))
            track.child(fill)
        }
        return track
    }

    private fun fraction(current: Int, goal: Int): Float =
        if (goal <= 0) 0f else (current.toFloat() / goal).coerceIn(0f, 1f)

    private fun isComplete(entry: RecipeCodexPacketEntry): Boolean = entry.goal > 0 && entry.current >= entry.goal

    private fun itemFor(key: String): ItemStack? {
        val id = ResourceLocation.tryParse(key) ?: return null
        return BuiltInRegistries.ITEM.getOptional(id).map { ItemStack(it) }.orElse(null)
    }

    private fun filterText(): Text = Text.literal(if (hideCompleted) "Show completed" else "Hide completed")


    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, Containers::verticalFlow)
    }

    override fun isPauseScreen(): Boolean = false

    private companion object {
        const val WIDTH = 240
        const val TEXT_WIDTH = 190
        const val BAR_HEIGHT = 5
        const val PAGE_SIZE = 30
        const val GRAY = 0xAAAAAA
        const val GREEN = 0x55FF55
        const val GOLD = 0xFFAA00
    }
}