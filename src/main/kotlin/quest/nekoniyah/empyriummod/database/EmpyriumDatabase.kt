package quest.nekoniyah.empyriummod.database

import com.mojang.serialization.JsonOps
import kotlinx.serialization.json.Json
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.economy.PlayerShopItem
import quest.nekoniyah.empyriummod.economy.currencies.Currency
import java.sql.Connection
import java.nio.file.Files
import java.nio.file.Path

object EmpyriumDatabase {
    var db: Database? = null
    fun init(dbDir: Path) {

        // Créer le répertoire dans world/ 
        Files.createDirectories(dbDir)

        // Récupère le path de la db
        val dbFile = dbDir.resolve("data.db")

        // Connexion à la db
        db = Database.connect("jdbc:sqlite:${dbFile.toAbsolutePath()}", "org.sqlite.JDBC")

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction(db) {
            SchemaUtils.create(PlayerShopTable)
            SchemaUtils.create(PlayerCacheTable)
        }
    }

    fun close() {
        db?.let { TransactionManager.closeAndUnregister(it) }
        db = null
    }

    fun getCachedPlayerName(uuid: String): String? {
        return transaction(db) {
            val alreadyCached = PlayerCacheTable.selectAll().where {
                PlayerCacheTable.uuid eq uuid
            }.firstOrNull()

            return@transaction alreadyCached?.get(PlayerCacheTable.name)
        }
    }

    fun cachePlayer(player: Player) {
        transaction(db) {
            PlayerCacheTable.upsert {
                it[uuid] = player.stringUUID
                it[name] = player.name.string
            }
        }
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

    fun decodeStringToItemStack(string: String): ItemStack {
        val pair =
            ItemStack.CODEC.decode(JsonOps.INSTANCE, Json.decodeFromString(string)).resultOrPartial().orElseThrow()
        return pair.first
    }

    fun getItems(): List<PlayerShopItem> {
        return transaction(db) {
            return@transaction PlayerShopTable.selectAll().orderBy(PlayerShopTable.id).toList().map { resultRow ->
                PlayerShopItem(
                    resultRow[PlayerShopTable.sellerUuid],
                    resultRow[PlayerShopTable.itemData],
                    resultRow[PlayerShopTable.itemCount],
                    resultRow[PlayerShopTable.currency],
                    resultRow[PlayerShopTable.price],
                    getCachedPlayerName(resultRow[PlayerShopTable.sellerUuid]) ?: "Unknown"
                )
            }
        }
    }
}