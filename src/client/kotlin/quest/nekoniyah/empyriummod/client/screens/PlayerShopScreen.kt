package quest.nekoniyah.empyriummod.client.screens

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import quest.nekoniyah.empyriummod.client.EmpyriumModClient
import quest.nekoniyah.empyriummod.client.util.PlayerShopActions
import quest.nekoniyah.empyriummod.client.util.ShopItemCodec
import quest.nekoniyah.empyriummod.client.util.ui.Helpers.button
import quest.nekoniyah.empyriummod.client.util.ui.Helpers.column
import quest.nekoniyah.empyriummod.client.util.ui.Helpers.label
import quest.nekoniyah.empyriummod.client.util.ui.Helpers.numberBox
import quest.nekoniyah.empyriummod.client.util.ui.Helpers.row
import quest.nekoniyah.empyriummod.economy.PlayerShopItem
import quest.nekoniyah.empyriummod.economy.currencies.CurrencyRegistry
import net.minecraft.network.chat.Component as Text

data class ShopResult(val success: Boolean, val message: String)

class PlayerShopScreen : EmpyriumScreen("Player Shop") {
    override val identifier: String = "player_shop"

    private enum class Tab(val title: String) {
        BROWSE("Browse"), MINE("My Listings"), SELL("Sell")
    }

    private enum class Kind { BUY, CANCEL }

    private data class Confirmation(val kind: Kind, val listing: PlayerShopItem)

    private val currencies = CurrencyRegistry.map { it.identifier }

    // State lives here so it survives rebuilding the UI
    private var tab = Tab.BROWSE
    private var search = ""
    private var confirmation: Confirmation? = null
    private var viewing: PlayerShopItem? = null // the listing open on the details page
    private var selectedSlot: Int? = null
    private var selectedStack = ItemStack.EMPTY
    private var countInput = "1"
    private var priceInput = ""
    private var currencyIndex = 0
    private var page = 0
    private var busy = false
    private var waitTicks = 0
    private var status: ShopResult? = null

    private var window: FlowLayout? = null
    private lateinit var list: FlowLayout
    private lateinit var footer: FlowLayout

    override fun build(rootComponent: FlowLayout) {
        rootComponent.surface(Surface.flat(0x77000000)).verticalAlignment(VerticalAlignment.CENTER)
            .horizontalAlignment(HorizontalAlignment.CENTER).sizing(Sizing.fill())

        val panel = Containers.verticalFlow(Sizing.fixed(WIDTH + 16), Sizing.content())
        panel.surface(Surface.DARK_PANEL).padding(Insets.of(8))
        panel.horizontalAlignment(HorizontalAlignment.CENTER)
        panel.gap(4)

        window = panel
        rootComponent.child(panel)
        refresh()
    }

    override fun tick() {
        super.tick()

        // Don't stay locked forever if the server never replies
        if (busy && ++waitTicks >= TIMEOUT_TICKS) {
            busy = false
            status = ShopResult(false, "No reply yet. Check your listings and balance before retrying.")
            refresh()
        }
    }

    /** Call from your packet receiver when the server replies to buy, sell or cancel. */
    fun onShopResult(result: ShopResult) {
        busy = false
        status = result
        confirmation = null

        if (result.success) {
            tab = Tab.BROWSE
            page = 0
            viewing = null
            selectedSlot = null
            priceInput = ""
        }
        refresh()
    }

    /** Locks transactions until the reply arrives, preventing double clicks. */
    private fun send(request: () -> Unit) {
        if (busy) return
        busy = true
        waitTicks = 0
        status = null
        refresh()
        request()
    }

    /** Rebuilds the window. Call this when new data arrives from the server. */
    fun refresh() {
        val panel = window ?: return
        panel.clearChildren()

        // Leave the details page if new data no longer contains this listing
        if (viewing != null && viewing !in allItems()) {
            viewing = null
            confirmation = null
        }

        panel.child(label("Player Shop"))
        panel.child(buildTabs())

        if (tab == Tab.BROWSE && viewing == null) panel.child(buildSearchBox())

        list = column(WIDTH)
        panel.child(
            Containers.verticalScroll(Sizing.fixed(WIDTH), Sizing.fixed(maxOf(70, height - 150)), list)
        )

        footer = column(WIDTH)
        panel.child(footer)

        refreshList()
        refreshFooter()
    }

    // ---------- Header ----------

    private fun buildTabs(): FlowLayout {
        val row = row()
        Tab.entries.forEach { target ->
            row.child(button(target.title, enabled = target != tab || viewing != null) {
                tab = target
                page = 0
                viewing = null
                confirmation = null
                status = null
                refresh()
            })
        }
        return row
    }

    private fun buildSearchBox(): Component {
        val box = Components.textBox(Sizing.fixed(WIDTH - 10), search)
        box.setMaxLength(50)
        box.setHint(Text.literal("Search this page..."))
        box.onChanged().subscribe { value ->
            search = value
            page = 0
            // Don't rebuild the whole window, so the text box keeps focus
            refreshList()
            refreshFooter()
        }
        return box
    }

    // ---------- Body ----------

    private fun refreshList() {
        list.clearChildren()

        viewing?.let {
            buildDetails(it)
            return
        }

        when (tab) {
            Tab.BROWSE -> buildBrowse()
            Tab.MINE -> buildMine()
            Tab.SELL -> buildInventory()
        }
    }

    /** All listings for the current tab, across every page. */
    private fun filteredListings(): List<PlayerShopItem> = when (tab) {
        Tab.BROWSE -> allItems().filterNot { isOwn(it) }
            .filter { decode(it).hoverName.string.contains(search.trim(), ignoreCase = true) }

        Tab.MINE -> allItems().filter { isOwn(it) }
        Tab.SELL -> emptyList()
    }

    private fun pageCount(total: Int): Int = maxOf(1, (total + PAGE_SIZE - 1) / PAGE_SIZE)

    private fun buildBrowse() = buildListings("No listings found.")

    private fun buildMine() = buildListings("You have no listings.")

    private fun buildListings(emptyMessage: String) {
        val listings = filteredListings()
        // The list may have shrunk, e.g. after buying the only item on the last page
        page = page.coerceIn(0, pageCount(listings.size) - 1)

        if (listings.isEmpty()) list.child(label(emptyMessage, GRAY))
        else list.child(listingGrid(listings.drop(page * PAGE_SIZE).take(PAGE_SIZE)))
    }

    private fun listingGrid(listings: List<PlayerShopItem>): Component {
        val rows = (listings.size + COLUMNS - 1) / COLUMNS
        val grid = Containers.grid(Sizing.content(), Sizing.content(), rows, COLUMNS)

        listings.forEachIndexed { index, listing ->
            grid.child(listingCell(listing), index / COLUMNS, index % COLUMNS)
        }
        return grid
    }

    private fun listingCell(listing: PlayerShopItem): FlowLayout {
        val stack = decode(listing)

        val cell = Containers.verticalFlow(Sizing.fixed(CELL), Sizing.fixed(CELL))
        cell.surface(Surface.flat(SLOT_COLOR))
        cell.horizontalAlignment(HorizontalAlignment.CENTER)
        cell.verticalAlignment(VerticalAlignment.CENTER)
        cell.margins(Insets.of(1))

        // The tooltip and click go on the cell, so the whole square responds.
        // The icon has no tooltip or overlay of its own.
        cell.tooltip(infoLines(listing, stack))
        cell.child(Components.item(stack))

        val count = label(listing.count.toString()).shadow(true)
        count.positioning(Positioning.relative(100, 100))
        count.zIndex(300) // otherwise the 3D item can draw over the text
        cell.child(count)

        cell.mouseEnter().subscribe { cell.surface(Surface.flat(HOVER_COLOR)) }
        cell.mouseLeave().subscribe { cell.surface(Surface.flat(SLOT_COLOR)) }
        cell.mouseDown().subscribe { _, _, button ->
            if (button == 0) {
                viewing = listing
                confirmation = null
                status = null
                refresh()
            }
            button == 0
        }
        return cell
    }

    /** Shared by the tooltip and the details page, so they always match. */
    private fun infoLines(listing: PlayerShopItem, stack: ItemStack): List<Text> {
        val lines = mutableListOf<Text>()

        if (ShopItemCodec.decode(listing.item) == null) {
            lines.add(Text.literal("This item couldn't be loaded"))
        } else {
            // Name, enchantments, lore, etc.
            lines.addAll(getTooltipFromItem(Minecraft.getInstance(), stack))
        }

        val seller = if (isOwn(listing)) "You" else listing.sellerName
        lines.add(gray("Quantity: ${listing.count}"))
        lines.add(gray("Price: ${listing.price} ${currencyName(listing.currency)}"))
        lines.add(gray("Seller: $seller"))
        return lines
    }

    // ---------- Details page ----------

    private fun buildDetails(listing: PlayerShopItem) {
        val stack = decode(listing)

        list.child(Components.item(stack).sizing(Sizing.fixed(32), Sizing.fixed(32)))

        infoLines(listing, stack).forEach { line ->
            list.child(Components.label(line).maxWidth(WIDTH - 20))
        }
    }

    private fun buildDetailActions(listing: PlayerShopItem) {
        val own = isOwn(listing)
        val loaded = ShopItemCodec.decode(listing.item) != null

        // Client-side check for the button only. The server must check the balance too.
        val currency = CurrencyRegistry.find { it.identifier == listing.currency }
        val balance = currency?.let { Minecraft.getInstance().player?.getAttached(it.ATTACHMENT) } ?: 0
        val enough = currency != null && balance >= listing.price

        if (!own) {
            val problem = when {
                !loaded -> "This item can't be bought because it couldn't be loaded."
                currency == null -> "Unknown currency."
                !enough -> "You don't have enough ${currency.name}."
                else -> null
            }
            problem?.let { footer.child(label(it, RED).maxWidth(WIDTH)) }
        }

        val buttons = row()
        buttons.child(button("Back") {
            viewing = null
            status = null
            refresh()
        })
        buttons.child(button(if (own) "Cancel listing" else "Buy", enabled = !busy && (own || (loaded && enough))) {
            confirmation = Confirmation(if (own) Kind.CANCEL else Kind.BUY, listing)
            status = null
            refreshFooter()
        })
        footer.child(buttons)
    }

    // ---------- Sell tab ----------

    private fun buildInventory() {
        val inventory = Minecraft.getInstance().player?.inventory ?: return

        list.child(label("Choose an item to sell:", GRAY))

        val grid = Containers.grid(Sizing.content(), Sizing.content(), 4, 9)
        for (slot in 0 until 36) {
            // Main inventory (9-35) on top, hotbar (0-8) on the bottom
            val gridRow = if (slot < 9) 3 else slot / 9 - 1
            val stack = inventory.getItem(slot)

            val cell = Containers.verticalFlow(Sizing.fixed(20), Sizing.fixed(20))
            cell.surface(Surface.flat(if (slot == selectedSlot) 0xFF55AA55.toInt() else 0x55000000))
            cell.horizontalAlignment(HorizontalAlignment.CENTER)
            cell.verticalAlignment(VerticalAlignment.CENTER)
            cell.margins(Insets.of(1))

            if (!stack.isEmpty) {
                val icon = Components.item(stack.copy()).showOverlay(true).setTooltipFromStack(true)
                icon.mouseDown().subscribe { _, _, _ ->
                    if (!busy) {
                        selectedSlot = slot
                        selectedStack = stack.copy()
                        countInput = stack.count.toString()
                        status = null
                        refreshList()
                        refreshFooter()
                    }
                    true
                }
                cell.child(icon)
            }
            grid.child(cell, gridRow, slot % 9)
        }
        list.child(grid)
    }

    private fun buildSellForm() {
        if (selectedSlot == null) return

        footer.child(label("Selling: ${selectedStack.hoverName.string}").maxWidth(WIDTH))

        val inputs = row()
        inputs.child(label("Qty"))
        inputs.child(numberBox(40, countInput) { countInput = it })
        inputs.child(label("Total price"))
        inputs.child(numberBox(70, priceInput) { priceInput = it })
        footer.child(inputs)

        val controls = row()
        controls.child(button(CurrencyRegistry[currencyIndex].name, enabled = !busy) {
            currencyIndex = (currencyIndex + 1) % currencies.size
            refreshFooter()
        })
        controls.child(button("List item", enabled = !busy) { submitSale() })
        footer.child(controls)
    }

    private fun submitSale() {
        val slot = selectedSlot ?: return
        val current = Minecraft.getInstance().player?.inventory?.getItem(slot) ?: return
        val count = countInput.toIntOrNull()
        val price = priceInput.toIntOrNull()

        // These checks are for user feedback only. The server must repeat them.
        val error = when {
            current.isEmpty || !ItemStack.matches(current, selectedStack) -> "That slot changed. Select the item again."

            count == null || count !in 1..current.count -> "Quantity must be 1-${current.count}."
            price == null || price <= 0 -> "Enter a price above 0."
            else -> null
        }

        if (current.isEmpty) {
            selectedSlot = null
            refreshList()
        }

        if (error != null || count == null || price == null) {
            status = ShopResult(false, error ?: "Invalid input.")
            refreshFooter()
            return
        }


        send { PlayerShopActions.sell(slot, count, currencies[currencyIndex], price) }
    }

    // ---------- Footer ----------

    private fun refreshFooter() {
        footer.clearChildren()

        val pending = confirmation
        val details = viewing
        when {
            tab == Tab.SELL -> buildSellForm()
            pending != null -> buildConfirmation(pending)
            details != null -> buildDetailActions(details)
            else -> footer.child(buildPagination())
        }

        if (busy) {
            footer.child(label("Waiting for server...", GRAY))
        } else {
            status?.let {
                footer.child(label(it.message, if (it.success) GREEN else RED).maxWidth(WIDTH))
            }
        }
    }

    private fun buildConfirmation(pending: Confirmation) {
        val listing = pending.listing
        val name = "${listing.count}x ${decode(listing).hoverName.string}"
        val question = when (pending.kind) {
            Kind.BUY -> "Buy $name for ${listing.price} ${currencyName(listing.currency)}?"
            Kind.CANCEL -> "Cancel your listing of $name?"
        }
        footer.child(label(question).maxWidth(WIDTH))

        val buttons = row()
        buttons.child(button("Confirm", enabled = !busy) {
            confirmation = null
            send {
                when (pending.kind) {
                    Kind.BUY -> PlayerShopActions.buy(listing)
                    Kind.CANCEL -> PlayerShopActions.cancel(listing)
                }
            }
        })

        buttons.child(button("Back") {
            confirmation = null
            refreshFooter()
        })
        footer.child(buttons)
    }

    private fun buildPagination(): FlowLayout {
        val pages = pageCount(filteredListings().size)
        val row = row()
        row.child(button("<", enabled = page > 0) { changePage(page - 1) })
        row.child(label("Page ${page + 1} / $pages"))
        row.child(button(">", enabled = page < pages - 1) { changePage(page + 1) })
        return row
    }

    private fun changePage(newPage: Int) {
        page = newPage
        status = null
        refresh()
    }

    // ---------- Data helpers ----------

    private fun allItems(): List<PlayerShopItem> = EmpyriumModClient.cachedPlayerShopItems

    private fun isOwn(listing: PlayerShopItem): Boolean =
        listing.sellerUuid == Minecraft.getInstance().player?.uuid?.toString()

    private fun currencyName(identifier: String): String =
        CurrencyRegistry.find { it.identifier == identifier }?.name ?: identifier

    private fun gray(text: String): Text = Text.literal(text).withStyle(ChatFormatting.GRAY)

    private fun decode(listing: PlayerShopItem): ItemStack =
        ShopItemCodec.decode(listing.item) ?: ItemStack(Items.BARRIER)


    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, Containers::verticalFlow)
    }

    companion object {
        const val WIDTH = 300
        private const val COLUMNS = 9
        private const val PAGE_SIZE = COLUMNS * 5
        private const val TIMEOUT_TICKS = 200 // about 10 seconds
        private const val CELL = 24
        private const val SLOT_COLOR = 0x55000000
        private const val HOVER_COLOR = 0x77FFFFFF
        const val WHITE = 0xFFFFFF
        const val GRAY = 0xAAAAAA
        const val GREEN = 0x55FF55
        const val RED = 0xFF5555
    }

    override fun isPauseScreen(): Boolean {
        return false
    }
}