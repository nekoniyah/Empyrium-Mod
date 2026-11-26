package quest.nekoniyah.empyriummod.client.screens

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import quest.nekoniyah.empyriummod.client.EmpyriumModClient
import quest.nekoniyah.empyriummod.client.util.PlayerShopActions
import quest.nekoniyah.empyriummod.client.util.ShopItemCodec
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
    private var selectedSlot: Int? = null
    private var selectedStack = ItemStack.EMPTY
    private var countInput = "1"
    private var priceInput = ""
    private var currencyIndex = 0
    private var busy = false
    private var status: ShopResult? = null

    private var window: FlowLayout? = null
    private lateinit var list: FlowLayout
    private lateinit var footer: FlowLayout

    override fun build(rootComponent: FlowLayout) {
        rootComponent
            .surface(Surface.flat(0x77000000))
            .verticalAlignment(VerticalAlignment.CENTER)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .sizing(Sizing.fill())

        val panel = Containers.verticalFlow(Sizing.fixed(WIDTH + 16), Sizing.content())
        panel.surface(Surface.DARK_PANEL).padding(Insets.of(8))
        panel.horizontalAlignment(HorizontalAlignment.CENTER)
        panel.gap(4)

        window = panel
        rootComponent.child(panel)
        refresh()
    }

    /** Rebuilds the window. Call this when new data arrives from the server. */
    fun refresh() {
        val panel = window ?: return
        panel.clearChildren()

        panel.child(label("Player Shop"))
        panel.child(buildTabs())

        if (tab == Tab.BROWSE) panel.child(buildSearchBox())

        list = column()
        panel.child(
            Containers.verticalScroll(Sizing.fixed(WIDTH), Sizing.fixed(maxOf(70, height - 150)), list)
        )

        footer = column()
        panel.child(footer)

        refreshList()
        refreshFooter()
    }

    // ---------- Header ----------

    private fun buildTabs(): FlowLayout {
        val row = row()
        Tab.entries.forEach { target ->
            row.child(button(target.title, enabled = target != tab) {
                tab = target
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
            // Only rebuild the list, so the text box keeps focus
            refreshList()
        }
        return box
    }

    // ---------- Body ----------

    private fun refreshList() {
        list.clearChildren()
        when (tab) {
            Tab.BROWSE -> buildBrowse()
            Tab.MINE -> buildMine()
            Tab.SELL -> buildInventory()
        }
    }

    private fun buildBrowse() {
        val listings = pageItems()
            .filterNot { isOwn(it) }
            .map { it to decode(it) }
            .filter { (_, stack) -> stack.hoverName.string.contains(search.trim(), ignoreCase = true) }

        if (listings.isEmpty()) list.child(label("No listings found on this page.", GRAY))
        listings.forEach { (listing, stack) ->
            list.child(listingRow(listing, stack, Kind.BUY))
        }
    }

    private fun buildMine() {
        val listings = pageItems().filter { isOwn(it) }

        if (listings.isEmpty()) list.child(label("You have no listings on this page.", GRAY))
        listings.forEach { list.child(listingRow(it, decode(it), Kind.CANCEL)) }
    }

    private fun listingRow(listing: PlayerShopItem, stack: ItemStack, kind: Kind): FlowLayout {
        val row = row()
        row.surface(Surface.flat(0x55000000)).padding(Insets.of(3))

        row.child(
            Components.item(stack.copy().also { it.count = listing.count })
                .showOverlay(true).setTooltipFromStack(true)
        )

        val info = Containers.verticalFlow(Sizing.fixed(190), Sizing.content())
        info.child(label("${listing.count}x ${stack.hoverName.string}").maxWidth(190))

        val seller = if (kind == Kind.BUY) " - ${listing.sellerName}" else ""
        info.child(label("${listing.price} ${listing.currency}$seller", GRAY).maxWidth(190))
        row.child(info)

        // Don't allow transactions on items we couldn't decode, except cancelling your own
        val valid = kind == Kind.CANCEL || ShopItemCodec.decode(listing.item) != null
        val text = if (kind == Kind.BUY) "Buy" else "Cancel"

        row.child(button(text, enabled = valid && !busy) {
            confirmation = Confirmation(kind, listing)
            status = null
            refreshFooter()
        })
        return row
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
        controls.child(button(currencies[currencyIndex], enabled = !busy) {
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
            current.isEmpty || !ItemStack.matches(current, selectedStack) ->
                "That slot changed. Select the item again."

            count == null || count !in 1..current.count -> "Quantity must be 1-${current.count}."
            price == null || price <= 0 -> "Enter a price above 0."
            else -> null
        }

        if (error != null || count == null || price == null) {
            status = ShopResult(false, error ?: "Invalid input.")
            refreshFooter()
            return
        }


        send { done ->
            PlayerShopActions.sell(slot, count, currencies[currencyIndex], price) { result ->
                if (result.success) {
                    selectedSlot = null
                    priceInput = ""
                }

                done(result)
            }
        }


    }

    // ---------- Footer ----------

    private fun refreshFooter() {
        footer.clearChildren()

        val pending = confirmation
        when {
            tab == Tab.SELL -> buildSellForm()
            pending != null -> buildConfirmation(pending)
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
            Kind.BUY -> "Buy $name for ${listing.price} ${listing.currency}?"
            Kind.CANCEL -> "Cancel your listing of $name?"
        }
        footer.child(label(question).maxWidth(WIDTH))

        val buttons = row()
        buttons.child(button("Confirm", enabled = !busy) {
            confirmation = null
            send { done ->
                when (pending.kind) {
                    Kind.BUY -> PlayerShopActions.buy(listing, done)
                    Kind.CANCEL -> PlayerShopActions.cancel(listing, done)
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
        val page = EmpyriumModClient.playerShopPage
        val row = row()
        row.child(button("<", enabled = page > 0) { changePage(page - 1) })
        row.child(label("Page ${page + 1}"))
        row.child(button(">", enabled = pageItems().isNotEmpty()) { changePage(page + 1) })
        return row
    }

    private fun changePage(page: Int) {
        EmpyriumModClient.playerShopPage = page
        status = null
        PlayerShopActions.requestPage(page)
        refresh()
    }

    /** Blocks other transactions until the result arrives, preventing double clicks. */
    private fun send(request: ((ShopResult) -> Unit) -> Unit) {
        if (busy) return
        busy = true
        status = null
        refresh()

        request { result ->
            busy = false
            status = result
            refresh()
        }
    }

    // ---------- Data helpers ----------

    private fun pageItems(): List<PlayerShopItem> =
        EmpyriumModClient.cachedPlayerShopItems[EmpyriumModClient.playerShopPage]?.toList().orEmpty()

    private fun isOwn(listing: PlayerShopItem): Boolean =
        listing.sellerUuid == Minecraft.getInstance().player?.uuid?.toString()

    private fun decode(listing: PlayerShopItem): ItemStack =
        ShopItemCodec.decode(listing.item) ?: ItemStack(Items.BARRIER)

    // ---------- UI helpers ----------

    private fun column(): FlowLayout {
        val flow = Containers.verticalFlow(Sizing.fixed(WIDTH), Sizing.content())
        flow.horizontalAlignment(HorizontalAlignment.CENTER)
        flow.gap(3)
        return flow
    }

    private fun row(): FlowLayout {
        val flow = Containers.horizontalFlow(Sizing.content(), Sizing.content())
        flow.verticalAlignment(VerticalAlignment.CENTER)
        flow.gap(4)
        return flow
    }

    private fun label(text: String, color: Int = WHITE) =
        Components.label(Text.literal(text)).color(Color.ofRgb(color))

    private fun button(text: String, enabled: Boolean = true, onClick: () -> Unit) =
        Components.button(Text.literal(text)) { onClick() }.also { it.active = enabled }

    private fun numberBox(width: Int, initial: String, onChange: (String) -> Unit): Component {
        val box = Components.textBox(Sizing.fixed(width))
        box.setMaxLength(9) // keeps the value within Int range
        box.setFilter { value -> value.all { it in '0'..'9' } }
        box.text(initial)
        box.onChanged().subscribe { onChange(it) }
        return box
    }

    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, Containers::verticalFlow)
    }

    private companion object {
        const val WIDTH = 300
        const val WHITE = 0xFFFFFF
        const val GRAY = 0xAAAAAA
        const val GREEN = 0x55FF55
        const val RED = 0xFF5555
    }
}