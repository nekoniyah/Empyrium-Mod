package quest.nekoniyah.empyriummod.database

import com.mojang.serialization.JsonOps
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase.db
import quest.nekoniyah.empyriummod.economy.PlayerShopItem
import quest.nekoniyah.empyriummod.economy.currencies.Currency

object PlayerShopTable : IntIdTable() {
    val itemData = text("item_data")
    val itemCount = integer("item_count")
    val sellerUuid = varchar("seller_uuid", 255)
    val currency = text("currency_identifier")
    val price = integer("price")

    fun removeItem(id: Int): Boolean {
        getItem(id) ?: return false
        return transaction(db) {
            val deletedRows = PlayerShopTable.deleteWhere {
                PlayerShopTable.id eq id
            }

            return@transaction deletedRows > 0
        }
    }

    fun getItem(id: Int): PlayerShopItem? {
        val res = transaction(db) {
            return@transaction PlayerShopTable.selectAll().where { PlayerShopTable.id eq id }.firstOrNull()
        }

        if (res == null) return null
        return PlayerShopItem(
            res[PlayerShopTable.id].value,
            res[PlayerShopTable.sellerUuid],
            res[PlayerShopTable.itemData],
            res[PlayerShopTable.itemCount],
            res[PlayerShopTable.currency],
            res[PlayerShopTable.price],
            PlayerCacheTable.getCachedPlayerName(res[PlayerShopTable.sellerUuid]) ?: "Unknown"
        )
    }

    fun sellItem(player: Player, itemstack: ItemStack, cur: Currency, curPrice: Int) {

        val jsonElement = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, itemstack).resultOrPartial().orElseThrow()

        val itemString = jsonElement.asJsonObject.toString()

        transaction(db) {
            PlayerShopTable.insert {
                it[sellerUuid] = player.stringUUID
                it[itemData] = itemString
                it[currency] = cur.identifier
                it[price] = curPrice
                it[itemCount] = itemstack.count
            }
        }
    }

    fun getItems(): List<PlayerShopItem> {
        return transaction(db) {
            return@transaction PlayerShopTable.selectAll().orderBy(PlayerShopTable.id).toList().map { resultRow ->
                PlayerShopItem(
                    resultRow[PlayerShopTable.id].value,
                    resultRow[sellerUuid],
                    resultRow[itemData],
                    resultRow[itemCount],
                    resultRow[currency],
                    resultRow[price],
                    PlayerCacheTable.getCachedPlayerName(resultRow[sellerUuid]) ?: "Unknown"
                )
            }
        }
    }
}