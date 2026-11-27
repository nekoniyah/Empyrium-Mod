package quest.nekoniyah.empyriummod.client.screens

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import net.minecraft.client.Minecraft
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

        val newSummary = column(WIDTH)
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

        val newList = column(WIDTH)
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
        val shown = visible.drop(page * PAGE_SIZE).take(PAGE_SIZE)
        if (shown.isNotEmpty()) list.child(entryGrid(shown))

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

    private fun entryGrid(entries: List<RecipeCodexPacketEntry>): Component {
        val rows = (entries.size + COLUMNS - 1) / COLUMNS
        val grid = Containers.grid(Sizing.content(), Sizing.content(), rows, COLUMNS)

        entries.forEachIndexed { index, entry ->
            grid.child(entryCell(entry), index / COLUMNS, index % COLUMNS)
        }
        return grid
    }

    private fun entryCell(entry: RecipeCodexPacketEntry): FlowLayout {
        val stack = itemFor(entry.itemKey)

        val cell = Containers.verticalFlow(Sizing.fixed(CELL), Sizing.fixed(CELL))
        cell.horizontalAlignment(HorizontalAlignment.CENTER)
        cell.verticalAlignment(VerticalAlignment.CENTER)
        cell.margins(Insets.of(1))

        // Gold: free craft ready. Green: completed.
        val color = when {
            hasFreeCraft(entry) -> 0x99FFAA00.toInt()
            isComplete(entry) -> 0x9933AA33.toInt()
            else -> 0x55000000
        }
        cell.surface(Surface.flat(color))

        // The tooltip is on the cell, so the whole square responds
        cell.tooltip(infoLines(entry, stack))
        cell.child(Components.item(stack ?: ItemStack(Items.BARRIER)))

        if (entry.current > 0) {
            val count = label(entry.current.toString()).shadow(true)
            count.positioning(Positioning.relative(100, 85))
            count.zIndex(300) // otherwise the 3D item can draw over the text
            cell.child(count)
        }

        if (entry.goal > 0 && entry.current > 0) {
            val bar = progressBar(CELL - 2, entry.current, entry.goal, isComplete(entry))
            bar.positioning(Positioning.relative(50, 100))
            bar.zIndex(250)
            cell.child(bar)
        }

        return cell
    }

    private fun infoLines(entry: RecipeCodexPacketEntry, stack: ItemStack?): List<Text> {
        val lines = mutableListOf<Text>()

        if (stack != null) lines.addAll(getTooltipFromItem(Minecraft.getInstance(), stack))
        else lines.add(Text.literal(entry.itemKey))

        val progress = if (entry.goal <= 0) "Crafted: ${entry.current} (no goal)"
        else if (isComplete(entry)) "Completed (${entry.current} / ${entry.goal})"
        else "${entry.current} / ${entry.goal} (${(fraction(entry.current, entry.goal) * 100).toInt()}%)"
        lines.add(colored(progress, if (isComplete(entry)) GREEN else GRAY))

        if (entry.totalFreeCount > 0) {
            val left = (entry.totalFreeCount - entry.usedFreeCount).coerceAtLeast(0)
            lines.add(colored("Free: $left / ${entry.totalFreeCount} left", if (left > 0) GOLD else GRAY))
        }

        return lines
    }

    private fun colored(text: String, color: Int): Text =
        Text.literal(text).withStyle { it.withColor(color) }

    private fun hasFreeCraft(entry: RecipeCodexPacketEntry): Boolean =
        isComplete(entry) && entry.usedFreeCount < entry.totalFreeCount

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


    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, Containers::verticalFlow)
    }

    override fun isPauseScreen(): Boolean = false

    private companion object {
        const val WIDTH = 240
        const val COLUMNS = 8
        const val CELL = 26
        const val BAR_HEIGHT = 5
        const val PAGE_SIZE = COLUMNS * 6
        const val GRAY = 0xAAAAAA
        const val GREEN = 0x55FF55
        const val GOLD = 0xFFAA00
    }
}