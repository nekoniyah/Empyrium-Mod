package quest.nekoniyah.empyriummod.client.screens

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.client.util.ShopItemCodec
import quest.nekoniyah.empyriummod.client.util.ui.Helpers
import quest.nekoniyah.empyriummod.client.util.ui.ShopItemForm
import quest.nekoniyah.empyriummod.client.util.ui.ShopLayout
import quest.nekoniyah.empyriummod.client.util.ui.ShopTheme
import quest.nekoniyah.empyriummod.economy.*
import quest.nekoniyah.empyriummod.economy.currencies.CurrencyRegistry
import quest.nekoniyah.empyriummod.network.packets.AdminShopRequestPacket
import net.minecraft.network.chat.Component as Text

object AdminShopClient {
    var categories: List<AdminShopCategoryEntry> = listOf()
        private set
    var items: List<AdminShopItemEntry> = listOf()
        private set

    /** Call from EmpyriumModClient.onInitializeClient. */
    fun register() {
        EmpyriumMod.NET_CHANNEL.registerClientbound(AdminShopDataPacket::class.java) { packet, _ ->
            categories = packet.categories
            items = packet.items
            (Minecraft.getInstance().screen as? AdminShopBaseScreen)?.refresh()
        }

        EmpyriumMod.NET_CHANNEL.registerClientbound(AdminShopResultPacket::class.java) { packet, _ ->
            (Minecraft.getInstance().screen as? AdminShopBaseScreen)?.onResult(packet)
        }
    }
}

/**
 * Shared three-column window:
 * categories (left), items (middle), details or editor (right).
 */
abstract class AdminShopBaseScreen(private val heading: String) : EmpyriumScreen(heading) {
    protected var categoryId: Int? = null
    protected var busy = false
        private set
    private var search = ""
    private var status: AdminShopResultPacket? = null
    private var waitTicks = 0
    private var confirmation: Confirmation? = null

    /** Updates text and buttons without rebuilding, so text boxes keep focus. */
    protected var liveUpdate: () -> Unit = {}

    protected lateinit var layout: ShopLayout
        private set
    private var root: FlowLayout? = null
    private var sidebar: FlowLayout? = null
    private var center: FlowLayout? = null
    private var side: FlowLayout? = null
    private var balanceLabel: LabelComponent? = null
    private var statusLabel: LabelComponent? = null

    private data class Confirmation(
        val question: String, val label: String, val color: Int, val onConfirm: () -> Unit
    )

    protected abstract fun buildCenter(pane: FlowLayout)

    /** content scrolls; actions stay pinned to the bottom. */
    protected abstract fun buildSide(content: FlowLayout, actions: FlowLayout)

    protected open fun buildSidebarActions(actions: FlowLayout, width: Int) {}
    protected open val sidebarActionHeight: Int = 0

    protected abstract fun onCategorySelected()
    protected abstract fun onSuccess()

    /** Drop any selection that no longer exists in the latest data. */
    protected abstract fun validateSelection()

    // ---------- Window ----------

    override fun build(rootComponent: FlowLayout) {
        root = rootComponent
        rootComponent.surface(Surface.flat(0x99000000.toInt()))
            .verticalAlignment(VerticalAlignment.CENTER)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .sizing(Sizing.fill())
        buildWindow()
    }

    private fun buildWindow() {
        val rootFlow = root ?: return
        layout = ShopLayout.from(width, height)
        rootFlow.clearChildren()

        val window = Containers.verticalFlow(Sizing.fixed(layout.width), Sizing.fixed(layout.height))
        window.surface(Helpers.bordered(ShopTheme.WINDOW, ShopTheme.BORDER))
        window.padding(Insets.of(6))
        window.gap(4)

        // Header: title on the left, balances on the right
        val header = Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(18))
        header.verticalAlignment(VerticalAlignment.CENTER)

        val titleBox = Containers.horizontalFlow(Sizing.fixed(90), Sizing.fixed(18))
        titleBox.verticalAlignment(VerticalAlignment.CENTER)
        titleBox.child(Helpers.label(heading, ShopTheme.GOLD).shadow(true))
        header.child(titleBox)

        val balanceBox = Containers.horizontalFlow(Sizing.fixed(layout.width - 102), Sizing.fixed(18))
        balanceBox.verticalAlignment(VerticalAlignment.CENTER)
        balanceBox.horizontalAlignment(HorizontalAlignment.RIGHT)
        val balances = Helpers.label("", ShopTheme.MUTED)
        balanceLabel = balances
        balanceBox.child(balances)
        header.child(balanceBox)
        window.child(header)

        val body = Containers.horizontalFlow(Sizing.content(), Sizing.fixed(layout.body))
        body.gap(4)
        sidebar = Helpers.pane(layout.sidebar, layout.body).also { body.child(it) }
        center = Helpers.pane(layout.center, layout.body).also { body.child(it) }
        side = Helpers.pane(layout.side, layout.body).also { body.child(it) }
        window.child(body)

        val message = Helpers.label("", ShopTheme.MUTED)
        statusLabel = message
        window.child(message)

        rootFlow.child(window)
        refresh()
    }

    fun refresh() {
        if (sidebar == null) return

        if (AdminShopClient.categories.none { it.id == categoryId }) {
            categoryId = AdminShopClient.categories.firstOrNull()?.id
        }
        validateSelection()

        renderSidebar()
        renderCenter()
        renderSide()
        renderStatus()
        renderBalance()
    }

    // ---------- Sidebar ----------

    private fun renderSidebar() {
        val pane = sidebar ?: return
        pane.clearChildren()

        val inner = layout.sidebar - 8
        val list = column(inner - 4)

        AdminShopClient.categories.forEach { category ->
            val row = Helpers.slot(inner - 4, 20, { category.id == categoryId }) {
                if (!busy) selectCategory(category.id)
            }
            row.padding(Insets.horizontal(2))
            row.gap(3)
            row.tooltip(Text.literal(category.name))
            row.child(Components.item(iconOf(category.iconKey)))
            row.child(Helpers.label(font.plainSubstrByWidth(category.name, inner - 30), ShopTheme.TEXT))
            list.child(row)
        }
        if (AdminShopClient.categories.isEmpty()) {
            list.child(wrapped("No categories", inner - 4, ShopTheme.MUTED))
        }

        val reserved = if (sidebarActionHeight > 0) sidebarActionHeight + 3 else 0
        pane.child(Helpers.scroll(inner, layout.body - 8 - reserved, list))

        if (sidebarActionHeight > 0) {
            val actions = column(inner)
            buildSidebarActions(actions, inner)
            pane.child(actions)
        }
    }

    private fun selectCategory(id: Int) {
        categoryId = id
        search = ""
        confirmation = null
        status = null
        onCategorySelected()
        refresh()
    }

    // ---------- Panes ----------

    protected fun renderCenter() {
        val pane = center ?: return
        pane.clearChildren()
        buildCenter(pane)
    }

    protected fun renderSide() {
        val pane = side ?: return
        pane.clearChildren()
        liveUpdate = {}

        val inner = layout.side - 8
        val content = column(inner - 4)
        val actions = column(inner)

        val pending = confirmation
        if (pending != null) {
            content.child(wrapped(pending.question, inner - 8, ShopTheme.TEXT))
            actions.child(Helpers.flatButton(pending.label, inner, pending.color, !busy) {
                confirmation = null
                pending.onConfirm()
            })
            actions.child(Helpers.flatButton("Cancel", inner) {
                confirmation = null
                renderSide()
            })
        } else {
            buildSide(content, actions)
        }

        // Two rows of buttons: 16 + 3 + 16
        pane.child(Helpers.scroll(inner, layout.body - 8 - 38, content))
        pane.child(actions)
        liveUpdate()
    }

    /** Search box above a scrolling grid of the selected category's items. */
    protected fun itemBrowser(
        pane: FlowLayout, selected: () -> Int?,
        addTile: (() -> Unit)? = null, onClick: (AdminShopItemEntry) -> Unit
    ) {
        val inner = layout.center - 8
        val gridHost = column(inner - 4)

        fun fillGrid() {
            gridHost.clearChildren()
            val query = search.trim()
            val entries = AdminShopClient.items
                .filter { it.categoryId == categoryId }
                .filter { decode(it).hoverName.string.contains(query, ignoreCase = true) }

            val cells = mutableListOf<FlowLayout>()
            if (addTile != null && categoryId != null) {
                val tile = itemSlot(null, { false }, addTile)
                tile.child(Helpers.label("+", ShopTheme.GOLD))
                tile.tooltip(Text.literal("Add an item"))
                cells.add(tile)
            }

            entries.forEach { entry ->
                val cell = itemSlot(decode(entry), { selected() == entry.id }) {
                    confirmation = null
                    status = null
                    onClick(entry)
                    renderStatus()
                }
                cell.tooltip(infoLines(entry))
                if (entry.count > 1) cell.child(countLabel(entry.count))
                cells.add(cell)
            }

            if (cells.isEmpty()) gridHost.child(wrapped("No items", inner - 8, ShopTheme.MUTED))
            else gridHost.child(gridOf(cells))
        }

        pane.child(Helpers.textBox(inner, search, hint = "Search...") {
            search = it
            fillGrid()
        })
        fillGrid()
        // 8 padding + 20 search box + 3 gap
        pane.child(Helpers.scroll(inner, layout.body - 31, gridHost))
    }

    /** Non-empty inventory slots, hotbar first, wrapped to fit the middle pane. */
    protected fun inventoryBrowser(
        pane: FlowLayout, title: String, selected: () -> Int, onPick: (Int, ItemStack) -> Unit
    ) {
        val inner = layout.center - 8
        pane.child(wrapped(title, inner, ShopTheme.GOLD))

        val inventory = Minecraft.getInstance().player?.inventory ?: return
        val cells = (0 until 36).mapNotNull { index ->
            val stack = inventory.getItem(index).copy()
            if (stack.isEmpty) return@mapNotNull null

            val cell = itemSlot(stack, { selected() == index }) { onPick(index, stack) }
            cell.tooltip(getTooltipFromItem(Minecraft.getInstance(), stack))
            if (stack.count > 1) cell.child(countLabel(stack.count))
            cell
        }

        val host = column(inner - 4)
        if (cells.isEmpty()) host.child(wrapped("Your inventory is empty", inner - 8, ShopTheme.MUTED))
        else host.child(gridOf(cells))
        // Leaves room for a title of up to two lines
        pane.child(Helpers.scroll(inner, layout.body - 33, host))
    }

    private fun itemSlot(stack: ItemStack?, selected: () -> Boolean, onClick: () -> Unit): FlowLayout {
        val cell = Helpers.slot(20, 20, selected) { if (!busy) onClick() }
        cell.horizontalAlignment(HorizontalAlignment.CENTER)
        if (stack != null) cell.child(Components.item(stack))
        return cell
    }

    private fun countLabel(count: Int): LabelComponent {
        val label = Helpers.label(count.toString(), ShopTheme.TEXT).shadow(true)
        label.positioning(Positioning.relative(100, 100))
        label.zIndex(300) // keeps the number above the 3D item
        return label
    }

    private fun gridOf(cells: List<FlowLayout>): Component {
        val columns = layout.columns
        val grid = Containers.grid(
            Sizing.content(), Sizing.content(), (cells.size + columns - 1) / columns, columns
        )
        cells.forEachIndexed { index, cell ->
            cell.margins(Insets.of(1))
            grid.child(cell, index / columns, index % columns)
        }
        return grid
    }

    // ---------- Header and status ----------

    /** Changing a label recalculates the layout, so skip it when nothing changed. */
    protected fun updateText(label: LabelComponent, value: String) {
        val next = Text.literal(value)
        if (label.text() != next) label.text(next)
    }

    private fun renderBalance() {
        val text = CurrencyRegistry.joinToString("   ") { "${balanceOf(it.identifier)} ${it.name}" }
        balanceLabel?.let { updateText(it, text) }
    }

    private fun renderStatus() {
        val message = when {
            busy -> "Waiting for server..."
            else -> status?.message.orEmpty()
        }
        val color = when {
            busy -> ShopTheme.MUTED
            status?.success == true -> ShopTheme.GOOD
            else -> ShopTheme.BAD
        }
        statusLabel?.text(Text.literal(font.plainSubstrByWidth(message, layout.width - 14)))
        statusLabel?.color(Color.ofRgb(color))
    }

    // ---------- Requests ----------

    protected fun ask(question: String, label: String, color: Int, onConfirm: () -> Unit) {
        confirmation = Confirmation(question, label, color, onConfirm)
        status = null
        renderSide()
        renderStatus()
    }

    protected fun clearConfirmation() {
        confirmation = null
    }

    protected fun clearStatus() {
        status = null
        renderStatus()
    }

    protected fun showError(message: String) {
        status = AdminShopResultPacket(false, message)
        renderStatus()
    }

    /** Sends one request and blocks further ones until the reply. */
    protected fun request(send: () -> Unit) {
        if (busy) return
        busy = true
        waitTicks = 0
        status = null
        refresh()
        send()
    }

    fun onResult(result: AdminShopResultPacket) {
        busy = false
        status = result
        if (result.success) onSuccess()
        refresh()
    }

    override fun tick() {
        super.tick()
        if (!::layout.isInitialized) return

        // owo keeps components when the window is resized, so rebuild for the new size
        if (layout != ShopLayout.from(width, height)) buildWindow()

        renderBalance()
        liveUpdate()

        if (busy && ++waitTicks >= 200) {
            busy = false
            status = AdminShopResultPacket(false, "No reply yet. Check before retrying.")
            refresh()
        }
    }

    // ---------- Data ----------

    protected fun decode(entry: AdminShopItemEntry): ItemStack =
        ShopItemCodec.decode(entry.item) ?: ItemStack(Items.BARRIER)

    protected fun iconOf(key: String): ItemStack {
        val item = ResourceLocation.tryParse(key)
            ?.let { BuiltInRegistries.ITEM.getOptional(it).orElse(null) }
        return ItemStack(item ?: Items.CHEST)
    }

    protected fun balanceOf(identifier: String): Int {
        val currency = CurrencyRegistry.find { it.identifier == identifier } ?: return 0
        return Minecraft.getInstance().player?.getAttached(currency.ATTACHMENT) ?: 0
    }

    protected fun currencyName(identifier: String): String =
        CurrencyRegistry.find { it.identifier == identifier }?.name ?: identifier

    private fun infoLines(entry: AdminShopItemEntry): List<Text> {
        val lines = mutableListOf<Text>()
        lines.addAll(getTooltipFromItem(Minecraft.getInstance(), decode(entry)))
        val currency = currencyName(entry.currency)

        fun gray(value: String) = Text.literal(value).withStyle(ChatFormatting.GRAY)
        lines.add(gray("Amount per trade: ${entry.count}"))
        lines.add(gray(if (entry.buyPrice > 0) "Buy for: ${entry.buyPrice} $currency" else "Can't be bought"))
        lines.add(gray(if (entry.sellPrice > 0) "Sell for: ${entry.sellPrice} $currency" else "Can't be sold"))
        return lines
    }

    // ---------- Layout ----------

    protected fun column(width: Int): FlowLayout = Helpers.column(width)

    protected fun wrapped(text: String, width: Int, color: Int): LabelComponent =
        Helpers.label(text, color).maxWidth(width)

    protected fun bigIcon(stack: ItemStack): Component =
        Components.item(stack).sizing(Sizing.fixed(32), Sizing.fixed(32))

    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, Containers::verticalFlow)
    }

    override fun isPauseScreen(): Boolean = false
}

/** Player view. */
class AdminShopScreen : AdminShopBaseScreen("Server Shop") {
    override val identifier: String = "admin_shop"

    private var selectedId: Int? = null
    private var quantity = "1"

    private fun selected(): AdminShopItemEntry? = AdminShopClient.items.find { it.id == selectedId }

    override fun validateSelection() {
        val item = selected()
        if (selectedId != null && (item == null || item.categoryId != categoryId)) {
            selectedId = null
            clearConfirmation()
        }
    }

    override fun onCategorySelected() {
        selectedId = null
    }

    // Stay on the item so the player can trade again
    override fun onSuccess() {}

    override fun buildCenter(pane: FlowLayout) {
        itemBrowser(pane, { selectedId }) {
            selectedId = it.id
            quantity = "1"
            renderSide()
        }
    }

    override fun buildSide(content: FlowLayout, actions: FlowLayout) {
        val width = layout.side - 8
        val textWidth = width - 8
        val item = selected()
        if (item == null) {
            content.child(wrapped("Select an item to see its prices.", textWidth, ShopTheme.MUTED))
            return
        }

        val stack = decode(item)
        val currency = currencyName(item.currency)

        content.child(bigIcon(stack))
        content.child(wrapped(stack.hoverName.string, textWidth, ShopTheme.TEXT))
        content.child(wrapped("${item.count} per trade", textWidth, ShopTheme.MUTED))

        // Quantity: [-] [number] [+]
        val box = Helpers.textBox(40, quantity, maxLength = 2, digitsOnly = true) {
            quantity = it
            liveUpdate()
        }
        fun step(change: Int) {
            box.text(((quantity.toIntOrNull() ?: 1) + change).coerceIn(1, 64).toString())
        }

        val stepper = Helpers.row()
        stepper.child(Helpers.flatButton("-", 18) { step(-1) })
        stepper.child(box)
        stepper.child(Helpers.flatButton("+", 18) { step(1) })
        content.child(wrapped("Trades", textWidth, ShopTheme.MUTED))
        content.child(stepper)

        val summary = wrapped("", textWidth, ShopTheme.GOLD)
        content.child(summary)

        val buy = Helpers.flatButton("Buy", width, ShopTheme.BUY) { confirmTrade(item, true) }
        val sell = Helpers.flatButton("Sell", width, ShopTheme.SELL) { confirmTrade(item, false) }
        actions.child(buy)
        actions.child(sell)

        liveUpdate = {
            val trades = quantity.toIntOrNull()?.takeIf { it in 1..64 }
            val buyTotal = if (trades != null && item.buyPrice > 0) item.buyPrice.toLong() * trades else null
            val sellTotal = if (trades != null && item.sellPrice > 0) item.sellPrice.toLong() * trades else null
            val affordable = buyTotal != null && balanceOf(item.currency) >= buyTotal

            val lines = mutableListOf<String>()
            if (trades == null) lines.add("Enter 1-64 trades")
            else lines.add("${item.count * trades} items")
            if (buyTotal != null) lines.add("Cost: $buyTotal $currency")
            if (sellTotal != null) lines.add("Earn: $sellTotal $currency")
            if (buyTotal != null && !affordable) lines.add("Not enough to buy")
            updateText(summary, lines.joinToString("\n"))

            buy.message = Text.literal(if (item.buyPrice > 0) "Buy" else "Not for sale")
            sell.message = Text.literal(if (item.sellPrice > 0) "Sell" else "Can't sell")
            buy.active = !busy && affordable
            sell.active = !busy && sellTotal != null
        }
    }

    private fun confirmTrade(item: AdminShopItemEntry, buying: Boolean) {
        val trades = quantity.toIntOrNull()
        if (trades == null || trades !in 1..64) {
            showError("Enter a number from 1 to 64.")
            return
        }

        val total = (if (buying) item.buyPrice else item.sellPrice).toLong() * trades
        val name = "${item.count.toLong() * trades}x ${decode(item).hoverName.string}"
        val verb = if (buying) "Buy" else "Sell"

        ask(
            "$verb $name for $total ${currencyName(item.currency)}?",
            "Confirm ${verb.lowercase()}",
            if (buying) ShopTheme.BUY else ShopTheme.SELL
        ) {
            request {
                EmpyriumMod.NET_CHANNEL.clientHandle().send(AdminShopTradePacket(item.id, buying, trades))
            }
        }
    }

    override fun init() {
        super.init()
        refresh() // show cached data straight away
        EmpyriumMod.NET_CHANNEL.clientHandle().send(AdminShopRequestPacket(0))
    }
}

/** Admin view. The server checks permissions for every change. */
class AdminShopEditorScreen : AdminShopBaseScreen("Shop Editor") {
    override val identifier: String = "admin_shop_editor"

    private enum class Mode { BROWSE, ITEM, CATEGORY }

    private var mode = Mode.BROWSE
    private var form = ShopItemForm()
    private var categoryName = ""
    private var iconSlot = -1

    // "+ Category" and the move/delete row
    override val sidebarActionHeight: Int = 35

    override fun validateSelection() {
        if (mode != Mode.ITEM) return
        val gone = if (form.isNew) categoryId == null
        else AdminShopClient.items.none { it.id == form.itemId && it.categoryId == categoryId }

        if (gone) {
            mode = Mode.BROWSE
            clearConfirmation()
        }
    }

    override fun onCategorySelected() {
        mode = Mode.BROWSE
    }

    override fun onSuccess() {
        mode = Mode.BROWSE
    }

    // ---------- Sidebar ----------

    override fun buildSidebarActions(actions: FlowLayout, width: Int) {
        actions.child(Helpers.flatButton("+ Category", width, enabled = !busy) {
            mode = Mode.CATEGORY
            categoryName = ""
            iconSlot = -1
            reopen()
        })

        val categories = AdminShopClient.categories
        val index = categories.indexOfFirst { it.id == categoryId }
        val category = categories.getOrNull(index)
        val third = (width - 4) / 3

        val tools = Containers.horizontalFlow(Sizing.content(), Sizing.content())
        tools.gap(2)

        val up = Helpers.flatButton("▲", third, enabled = !busy && index > 0) {
            category?.let { send(AdminShopMoveCategoryPacket(it.id, -1)) }
        }
        up.tooltip(Text.literal("Move category up"))

        val down = Helpers.flatButton("▼", third, enabled = !busy && index in 0 until categories.size - 1) {
            category?.let { send(AdminShopMoveCategoryPacket(it.id, 1)) }
        }
        down.tooltip(Text.literal("Move category down"))

        val delete = Helpers.flatButton("✕", third, ShopTheme.DANGER, !busy && category != null) {
            category?.let {
                ask("Delete '${it.name}' and ALL its items?", "Delete category", ShopTheme.DANGER) {
                    send(AdminShopDeleteCategoryPacket(it.id))
                }
            }
        }
        delete.tooltip(Text.literal("Delete category"))

        tools.child(up)
        tools.child(down)
        tools.child(delete)
        actions.child(tools)
    }

    // ---------- Middle ----------

    override fun buildCenter(pane: FlowLayout) {
        when {
            mode == Mode.CATEGORY -> inventoryBrowser(pane, "Pick an icon", { iconSlot }) { picked, _ ->
                // Click again to go back to the default chest
                iconSlot = if (iconSlot == picked) -1 else picked
            }

            mode == Mode.ITEM && form.isNew -> inventoryBrowser(pane, "Pick an item", { form.slot }) { picked, stack ->
                form.slot = picked
                form.itemName = stack.hoverName.string
                form.maxAmount = stack.maxStackSize
                form.amount = stack.count.toString()
                renderSide()
            }

            else -> itemBrowser(
                pane,
                { if (mode == Mode.ITEM) form.itemId else null },
                addTile = { openForm(null) }
            ) { openForm(it) }
        }
    }

    private fun openForm(entry: AdminShopItemEntry?) {
        // The middle pane only changes when entering or leaving an inventory picker
        val switchPane = entry == null || mode == Mode.CATEGORY || (mode == Mode.ITEM && form.isNew)
        form = if (entry == null) {
            ShopItemForm(currency = CurrencyRegistry.firstOrNull()?.identifier.orEmpty())
        } else {
            val stack = decode(entry)
            ShopItemForm(
                itemId = entry.id,
                itemName = stack.hoverName.string,
                maxAmount = stack.maxStackSize,
                amount = entry.count.toString(),
                currency = entry.currency,
                buyPrice = entry.buyPrice.takeIf { it > 0 }?.toString().orEmpty(),
                sellPrice = entry.sellPrice.takeIf { it > 0 }?.toString().orEmpty()
            )
        }

        mode = Mode.ITEM
        clearConfirmation()
        clearStatus()
        // Keep the grid (and its scroll position) when editing an existing item
        if (switchPane) renderCenter()
        renderSide()
    }

    private fun reopen() {
        clearConfirmation()
        clearStatus()
        renderCenter()
        renderSide()
    }

    // ---------- Right ----------

    override fun buildSide(content: FlowLayout, actions: FlowLayout) {
        val width = layout.side - 8
        when (mode) {
            Mode.BROWSE -> content.child(
                wrapped("Select an item to edit it, or press + to add one.", width - 8, ShopTheme.MUTED)
            )
            Mode.ITEM -> buildItemForm(content, actions, width)
            Mode.CATEGORY -> buildCategoryForm(content, actions, width)
        }
    }

    private fun buildItemForm(content: FlowLayout, actions: FlowLayout, width: Int) {
        val textWidth = width - 8
        val existing = AdminShopClient.items.find { it.id == form.itemId }
        val inventory = Minecraft.getInstance().player?.inventory

        val icon = when {
            existing != null -> decode(existing)
            form.slot >= 0 -> inventory?.getItem(form.slot)?.copy()
            else -> null
        }
        if (icon != null) content.child(bigIcon(icon))
        content.child(
            wrapped(
                form.itemName.ifEmpty { "Pick an item from your inventory. It won't be taken." },
                textWidth, if (form.itemName.isEmpty()) ShopTheme.MUTED else ShopTheme.TEXT
            )
        )

        fun field(title: String, value: String, hint: String, onChange: (String) -> Unit) {
            content.child(wrapped(title, textWidth, ShopTheme.MUTED))
            content.child(
                Helpers.textBox(textWidth, value, maxLength = 9, digitsOnly = true, hint = hint, onChange = onChange)
            )
        }

        field("Amount per trade", form.amount, "1") { form.amount = it }
        field("Player pays", form.buyPrice, "Disabled") { form.buyPrice = it }
        field("Player receives", form.sellPrice, "Disabled") { form.sellPrice = it }

        content.child(wrapped("Currency", textWidth, ShopTheme.MUTED))
        content.child(Helpers.flatButton(currencyName(form.currency), textWidth, enabled = !busy) { button ->
            val ids = CurrencyRegistry.map { it.identifier }
            if (ids.isNotEmpty()) form.currency = ids[(ids.indexOf(form.currency) + 1) % ids.size]
            button.message = Text.literal(currencyName(form.currency))
        })

        actions.child(Helpers.flatButton("Save", width, ShopTheme.BUY, !busy) { saveItem() })

        if (form.isNew) {
            actions.child(Helpers.flatButton("Cancel", width) { close(Mode.BROWSE) })
        } else {
            val id = form.itemId
            actions.child(Helpers.flatButton("Remove", width, ShopTheme.DANGER, !busy) {
                ask("Remove ${form.itemName} from the shop?", "Remove item", ShopTheme.DANGER) {
                    send(AdminShopRemoveItemPacket(id))
                }
            })
        }
    }

    private fun buildCategoryForm(content: FlowLayout, actions: FlowLayout, width: Int) {
        val textWidth = width - 8
        content.child(wrapped("New category", textWidth, ShopTheme.TEXT))
        content.child(Helpers.textBox(textWidth, categoryName, hint = "Name") { categoryName = it })
        content.child(wrapped("The icon is optional. The default is a chest.", textWidth, ShopTheme.MUTED))

        actions.child(Helpers.flatButton("Create", width, ShopTheme.BUY, !busy) {
            if (categoryName.isBlank()) showError("Enter a name.")
            else send(AdminShopCreateCategoryPacket(categoryName.trim(), iconSlot))
        })
        actions.child(Helpers.flatButton("Cancel", width) { close(Mode.BROWSE) })
    }

    private fun close(next: Mode) {
        mode = next
        reopen()
    }

    private fun saveItem() {
        val category = categoryId ?: return
        val error = form.error()
        val amount = form.amount.toIntOrNull()

        if (error != null || amount == null) {
            showError(error ?: "Invalid amount.")
            return
        }

        send(
            AdminShopSaveItemPacket(
                form.itemId, category, form.slot, amount, form.currency,
                form.parsePrice(form.buyPrice), form.parsePrice(form.sellPrice)
            )
        )
    }

    private fun send(packet: Record) {
        request { EmpyriumMod.NET_CHANNEL.clientHandle().send(packet) }
    }
}