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

        val tracked = entries.filter { it.max > 0 }
        val maxed = tracked.count { isMax(it) }
        summary.clearChildren()
        summary.child(label("$maxed / ${tracked.size} recipes maxed", GRAY))
        summary.child(progressBar(WIDTH - 20, maxed, tracked.size, maxed == tracked.size && maxed > 0))

        val visible = entries.filter { matchesSearch(it) }
            .sortedWith(compareBy<RecipeCodexPacketEntry> { it.max <= 0 }.thenBy { isMax(it) }
                .thenByDescending { fraction(it.current, it.max) }.thenBy { it.itemKey })

        list.clearChildren()

        if (visible.isEmpty()) {
            val message = when {
                entries.isEmpty() -> "No recipes to show."
                search.isNotBlank() -> "No matching recipes."
                else -> "You maxed all recipes!"
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

        val color = when {
            isMax(entry) -> 0x9933AA33.toInt()
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

        if (entry.max > 0 && entry.current > 0) {
            val bar = progressBar(CELL - 2, entry.current, entry.max, isMax(entry))
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

        val progress = if (entry.max <= 0) "Crafted: ${entry.current}"
        else if (isMax(entry)) "Maxed: ${entry.current} / ${entry.max}"
        else "${entry.current} / ${entry.max} (${
            (fraction(
                entry.current, entry.max
            ) * 100).toInt()
        }% chance for a free craft.)"

        lines.add(colored(progress, if (isMax(entry)) GREEN else GRAY))

        return lines
    }

    private fun colored(text: String, color: Int): Text = Text.literal(text).withStyle { it.withColor(color) }

    private fun progressBar(width: Int, current: Int, goal: Int, reachedMax: Boolean): FlowLayout {
        val track = Containers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(BAR_HEIGHT))
        track.surface(Surface.flat(0xFF333333.toInt()))

        val filled = (width * fraction(current, goal)).toInt()
        if (filled > 0) {
            val fill = Containers.horizontalFlow(Sizing.fixed(filled), Sizing.fixed(BAR_HEIGHT))
            val color = if (reachedMax) 0xFF55FF55 else 0xFF55AAFF
            fill.surface(Surface.flat(color.toInt()))
            track.child(fill)
        }
        return track
    }

    private fun fraction(current: Int, goal: Int): Float =
        if (goal <= 0) 0f else (current.toFloat() / goal).coerceIn(0f, 1f)

    private fun isMax(entry: RecipeCodexPacketEntry): Boolean = entry.max > 0 && entry.current >= entry.max

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
    }
}