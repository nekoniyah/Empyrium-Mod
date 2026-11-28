package quest.nekoniyah.empyriummod.economy

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import quest.nekoniyah.empyriummod.EmpyriumMod.NET_CHANNEL
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase
import quest.nekoniyah.empyriummod.database.economy.AdminShopCategoryTable
import quest.nekoniyah.empyriummod.database.economy.AdminShopTable
import quest.nekoniyah.empyriummod.economy.currencies.CurrencyRegistry
import quest.nekoniyah.empyriummod.network.packets.AdminShopRequestPacket
import quest.nekoniyah.empyriummod.network.packets.OpenScreenPacket

// A price of -1 means "not available"
@JvmRecord
data class AdminShopCategoryEntry(val id: Int, val name: String, val iconKey: String)

@JvmRecord
data class AdminShopItemEntry(
    val id: Int, val categoryId: Int, val item: String, val count: Int,
    val currency: String, val buyPrice: Int, val sellPrice: Int
)

// Server -> client
@JvmRecord
data class AdminShopDataPacket(val categories: List<AdminShopCategoryEntry>, val items: List<AdminShopItemEntry>)

@JvmRecord
data class AdminShopResultPacket(val success: Boolean, val message: String)

// Client -> server (player)
/** buying = true: the player buys from the shop. false: the player sells to it. */
@JvmRecord
data class AdminShopTradePacket(val itemId: Int, val buying: Boolean, val quantity: Int)

// Client -> server (admin)
/** itemId -1 creates a new item from the inventory slot. When editing, slot is ignored. */
@JvmRecord
data class AdminShopSaveItemPacket(
    val itemId: Int, val categoryId: Int, val slot: Int, val count: Int,
    val currency: String, val buyPrice: Int, val sellPrice: Int
)

@JvmRecord
data class AdminShopRemoveItemPacket(val itemId: Int)

/** iconSlot -1 uses the default chest icon. */
@JvmRecord
data class AdminShopCreateCategoryPacket(val name: String, val iconSlot: Int)

/** direction: -1 = left, 1 = right */
@JvmRecord
data class AdminShopMoveCategoryPacket(val categoryId: Int, val direction: Int)

@JvmRecord
data class AdminShopDeleteCategoryPacket(val categoryId: Int)

/** Extra queries for the admin shop. Your table objects stay unchanged. */
object AdminShopQueries {
    fun getItems(): List<AdminShopItemEntry> = transaction(EmpyriumDatabase.db) {
        AdminShopTable.selectAll().orderBy(AdminShopTable.id).map { r ->
            AdminShopItemEntry(
                r[AdminShopTable.id].value,
                r[AdminShopTable.category].value,
                r[AdminShopTable.itemData],
                r[AdminShopTable.itemCount],
                r[AdminShopTable.currency],
                r[AdminShopTable.buyPrice] ?: -1,
                r[AdminShopTable.sellPrice] ?: -1
            )
        }
    }

    /** Categories in display order. */
    fun sortedCategories(): List<AdminShopCategoryTable.CategoryInfo> =
        AdminShopCategoryTable.getCategories().sortedWith(compareBy({ it.position }, { it.id }))

    fun updateItem(id: Int, count: Int, currency: String, buyPrice: Int?, sellPrice: Int?): Boolean =
        transaction(EmpyriumDatabase.db) {
            AdminShopTable.update({ AdminShopTable.id eq id }) {
                it[itemCount] = count
                it[AdminShopTable.currency] = currency
                it[AdminShopTable.buyPrice] = buyPrice
                it[AdminShopTable.sellPrice] = sellPrice
            } > 0
        }

    /** Renumbers every category, because new ones all start at position 1. */
    fun shiftCategory(id: Int, direction: Int): Boolean {
        val ids = sortedCategories().map { it.id }.toMutableList()
        val from = ids.indexOf(id)
        val to = from + direction
        if (from < 0 || to !in ids.indices) return false

        ids.add(to, ids.removeAt(from))
        return transaction(EmpyriumDatabase.db) {
            ids.forEachIndexed { index, categoryId ->
                AdminShopCategoryTable.moveCategory(categoryId, index + 1)
            }
            true
        }
    }

    fun deleteCategory(id: Int): Boolean = transaction(EmpyriumDatabase.db) {
        // Delete items explicitly: SQLite only cascades when foreign keys are enabled
        AdminShopTable.deleteWhere { AdminShopTable.category eq id }
        AdminShopCategoryTable.deleteWhere { AdminShopCategoryTable.id eq id } > 0
    }
}

object AdminShopServer {
    private const val ADMIN_LEVEL = 4

    /** Call once from the COMMON initializer (EmpyriumMod.onInitialize). */
    fun register() {
        // Client handlers are added in AdminShopClient (AdminShopScreens.kt). Keep this on both sides.
        NET_CHANNEL.registerClientboundDeferred(AdminShopDataPacket::class.java)
        NET_CHANNEL.registerClientboundDeferred(AdminShopResultPacket::class.java)

        NET_CHANNEL.registerServerbound(AdminShopTradePacket::class.java) { packet, access ->
            trade(access.player(), packet)
        }
        NET_CHANNEL.registerServerbound(AdminShopSaveItemPacket::class.java) { packet, access ->
            adminAction(access.player()) { saveItem(it, packet) }
        }
        NET_CHANNEL.registerServerbound(AdminShopRemoveItemPacket::class.java) { packet, access ->
            adminAction(access.player()) {
                if (AdminShopTable.removeItem(packet.itemId)) ok("Item removed.") else fail("Item not found.")
            }
        }
        NET_CHANNEL.registerServerbound(AdminShopCreateCategoryPacket::class.java) { packet, access ->
            adminAction(access.player()) { createCategory(it, packet) }
        }
        NET_CHANNEL.registerServerbound(AdminShopMoveCategoryPacket::class.java) { packet, access ->
            adminAction(access.player()) {
                val valid = packet.direction == -1 || packet.direction == 1
                if (valid && AdminShopQueries.shiftCategory(packet.categoryId, packet.direction)) ok("Category moved.")
                else fail("Couldn't move category.")
            }
        }
        NET_CHANNEL.registerServerbound(AdminShopDeleteCategoryPacket::class.java) { packet, access ->
            adminAction(access.player()) {
                if (AdminShopQueries.deleteCategory(packet.categoryId)) ok("Category deleted.")
                else fail("Category not found.")
            }
        }

        NET_CHANNEL.registerServerbound(AdminShopRequestPacket::class.java) { _, access ->
            sendData(access.player())
        }
    }

    /** Open either view with this. */
    fun open(player: ServerPlayer, adminView: Boolean) {
        if (adminView && !player.hasPermissions(ADMIN_LEVEL)) return

        sendData(player)
        NET_CHANNEL.serverHandle(player)
            .send(OpenScreenPacket(if (adminView) "admin_shop_editor" else "admin_shop"))
    }

    private fun sendData(player: ServerPlayer) {
        val categories = AdminShopQueries.sortedCategories()
            .map { AdminShopCategoryEntry(it.id, it.name, it.itemIconKey) }

        NET_CHANNEL.serverHandle(player).send(AdminShopDataPacket(categories, AdminShopQueries.getItems()))
    }

    private fun ok(message: String) = AdminShopResultPacket(true, message)
    private fun fail(message: String) = AdminShopResultPacket(false, message)

    private fun reply(player: ServerPlayer, result: AdminShopResultPacket) {
        NET_CHANNEL.serverHandle(player).send(result)
    }

    private fun adminAction(player: ServerPlayer, action: (ServerPlayer) -> AdminShopResultPacket) {
        // Never rely on the client hiding the admin screen
        if (!player.hasPermissions(ADMIN_LEVEL)) {
            reply(player, fail("You don't have permission."))
            return
        }

        val result = runCatching { action(player) }.getOrElse {
            it.printStackTrace()
            fail("Server error. Check the server log.")
        }

        // Send fresh data to everyone, so open shops don't show old prices
        if (result.success) player.server.playerList.players.forEach { sendData(it) }
        reply(player, result)
    }

    private fun saveItem(player: ServerPlayer, packet: AdminShopSaveItemPacket): AdminShopResultPacket {
        if (CurrencyRegistry.none { it.identifier == packet.currency }) return fail("Unknown currency.")

        fun validPrice(price: Int) = price == -1 || price > 0
        if (!validPrice(packet.buyPrice) || !validPrice(packet.sellPrice)) return fail("Prices must be above 0.")
        if (packet.buyPrice == -1 && packet.sellPrice == -1) return fail("Set at least one price.")

        // If selling back pays more than buying costs, players get unlimited money
        if (packet.buyPrice != -1 && packet.sellPrice > packet.buyPrice) {
            return fail("The sell price can't be higher than the buy price.")
        }

        val buyPrice = packet.buyPrice.takeIf { it > 0 }
        val sellPrice = packet.sellPrice.takeIf { it > 0 }

        if (packet.itemId != -1) {
            val existing = AdminShopQueries.getItems().find { it.id == packet.itemId }
                ?: return fail("Item not found.")
            val stack = decode(player, existing.item) ?: return fail("Couldn't load the stored item.")
            if (packet.count !in 1..stack.maxStackSize) return fail("Count must be 1-${stack.maxStackSize}.")

            // item_count is the bundle size. The stored item data is left as it is.
            val updated = AdminShopQueries.updateItem(packet.itemId, packet.count, packet.currency, buyPrice, sellPrice)
            return if (updated) ok("Item updated.") else fail("Item not found.")
        }

        if (AdminShopCategoryTable.getCategory(packet.categoryId) == null) return fail("Category not found.")
        if (packet.slot !in 0 until 36) return fail("Choose an item.")

        // Read from the server's inventory. The admin's item isn't consumed.
        val source = player.inventory.getItem(packet.slot)
        if (source.isEmpty) return fail("That slot is empty.")
        if (packet.count !in 1..source.maxStackSize) return fail("Count must be 1-${source.maxStackSize}.")

        val ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE)
        val data = ItemStack.CODEC.encodeStart(ops, source.copyWithCount(1)).result().orElse(null)
            ?: return fail("Couldn't save that item.")

        val added = AdminShopTable.addItem(
            data.toString(), packet.count, packet.currency, packet.categoryId,
            sellPrice = sellPrice, buyPrice = buyPrice
        )
        return if (added) ok("Item added.") else fail("Couldn't add item.")
    }

    private fun decode(player: ServerPlayer, data: String): ItemStack? = runCatching {
        val ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE)
        ItemStack.CODEC.parse(ops, JsonParser.parseString(data)).result().orElse(null)
    }.getOrNull()

    private fun createCategory(player: ServerPlayer, packet: AdminShopCreateCategoryPacket): AdminShopResultPacket {
        val name = packet.name.trim()
        if (name.isEmpty() || name.length > 32) return fail("Name must be 1-32 characters.")

        val icon = if (packet.iconSlot in 0 until 36) player.inventory.getItem(packet.iconSlot) else ItemStack.EMPTY
        val item = if (icon.isEmpty) Items.CHEST else icon.item
        val last = AdminShopQueries.sortedCategories().maxOfOrNull { it.position } ?: 0

        val id = AdminShopCategoryTable.createCategory(name, BuiltInRegistries.ITEM.getKey(item).toString())
            ?: return fail("Couldn't create category.")
        AdminShopCategoryTable.moveCategory(id, last + 1)
        return ok("Category created.")
    }

    private fun trade(player: ServerPlayer, packet: AdminShopTradePacket) {
        // Price and item always come from the database, never from the client
        val entry = AdminShopQueries.getItems().find { it.id == packet.itemId }
        val price = if (packet.buying) entry?.buyPrice else entry?.sellPrice
        val total = (price ?: 0).toLong() * packet.quantity

        val result = when {
            entry == null -> fail("That item is no longer available.")
            price == null || price <= 0 -> fail("That trade isn't available.")
            packet.quantity !in 1..64 -> fail("Quantity must be 1-64.")
            total > Int.MAX_VALUE -> fail("That total is too large.")
            // TODO: replace with the real trade. Move entry.count * packet.quantity items.
            // Buying: check funds and inventory space, take `total`, give copies of the item.
            // Selling: check matching items (including components), remove them, pay `total`.
            else -> fail(if (packet.buying) "Buying isn't implemented yet." else "Selling isn't implemented yet.")
        }
        reply(player, result)
    }
}