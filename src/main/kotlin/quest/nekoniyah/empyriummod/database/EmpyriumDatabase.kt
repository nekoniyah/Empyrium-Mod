package quest.nekoniyah.empyriummod.database

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.EmpyriumMod.NET_CHANNEL
import quest.nekoniyah.empyriummod.config.RecipeCodexConfig
import quest.nekoniyah.empyriummod.economy.PlayerShopItem
import quest.nekoniyah.empyriummod.economy.currencies.Currency
import quest.nekoniyah.empyriummod.network.packets.RecipeCodexPacketEntry
import quest.nekoniyah.empyriummod.network.packets.RecipeCodexResponsePacket
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import kotlin.random.Random

object EmpyriumDatabase {
    var db: Database? = null
    fun init(dbDir: Path) {

        // Créer le répertoire dans data/
        Files.createDirectories(dbDir)

        // Récupère le path de la db
        val dbFile = dbDir.resolve("data.db")

        // Connexion à la db
        db = Database.connect("jdbc:sqlite:${dbFile.toAbsolutePath()}", "org.sqlite.JDBC")

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction(db) {
            SchemaUtils.create(PlayerShopTable, PlayerCacheTable, PlayerCraftsTable, CurrencyBalanceTable)
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
            PlayerCacheTable.upsert(body = {
                it[uuid] = player.stringUUID
                it[name] = player.name.string
            }, where = {
                PlayerCacheTable.uuid eq player.stringUUID
            })
        }
    }

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
            getCachedPlayerName(res[PlayerShopTable.sellerUuid]) ?: "Unknown"
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

    fun checkRecipeCodexForFreeCraft(player: Player, item: ItemStack): Boolean {
        val itemLocation = BuiltInRegistries.ITEM.getKey(item.item)

        val allCraftData = RecipeCodexConfig.getAll()
        val itemCraftInfo = allCraftData[itemLocation.toString()] ?: return false

        return transaction(db) {
            val existingRec = PlayerCraftsTable.selectAll()
                .where { (PlayerCraftsTable.playerUuid eq player.stringUUID) and (PlayerCraftsTable.itemId eq itemLocation.toString()) }
                .firstOrNull() ?: return@transaction false


            val hasDone100 = existingRec[PlayerCraftsTable.count] >= itemCraftInfo.max

            if (hasDone100) {
                transaction(db) {
                    PlayerCraftsTable.deleteWhere { (PlayerCraftsTable.playerUuid eq player.stringUUID) and (PlayerCraftsTable.itemId eq itemLocation.toString()) }
                }
            }

            val percentage = existingRec[PlayerCraftsTable.count]
            val rand = Random.nextInt(0, 100 + 1)

            return@transaction rand <= percentage
        }

    }


    fun addItemToRecipeCodex(player: Player, item: ItemStack): ResultRow? {
        return transaction(db) {
            return@transaction PlayerCraftsTable.upsert(body = {
                it[playerUuid] = player.stringUUID
                it[itemId] = BuiltInRegistries.ITEM.getKey(item.item).toString()
            }, where = {
                PlayerCraftsTable.playerUuid eq player.stringUUID
            }).resultedValues?.first()
        }
    }

    fun addOrUpdateItemToRecipeCodex(player: Player, item: ItemStack) {
        val itemKey = BuiltInRegistries.ITEM.getKey(item.item)
        transaction(db) {
            var existingRes = PlayerCraftsTable.selectAll()
                .where { (PlayerCraftsTable.playerUuid eq player.stringUUID) and (PlayerCraftsTable.itemId eq itemKey.toString()) }
                .firstOrNull()

            if (existingRes == null) {
                existingRes = addItemToRecipeCodex(player, item)!!
            }


            val conf =
                RecipeCodexConfig.getAll()[BuiltInRegistries.ITEM.getKey(item.item).toString()] ?: return@transaction

            if (existingRes[PlayerCraftsTable.count] >= conf.cap) return@transaction

            val newCount = existingRes[PlayerCraftsTable.count] + 1

            transaction(db) {
                PlayerCraftsTable.update({ (PlayerCraftsTable.playerUuid eq player.stringUUID) and (PlayerCraftsTable.itemId eq itemKey.toString()) }) {
                    it[count] = newCount
                }
            }
        }

        NET_CHANNEL.serverHandle(player)
            .send(RecipeCodexResponsePacket(getFullCodex(EmpyriumMod.server!!, player.stringUUID)))
    }

    fun getItems(): List<PlayerShopItem> {
        return transaction(db) {
            return@transaction PlayerShopTable.selectAll().orderBy(PlayerShopTable.id).toList().map { resultRow ->
                PlayerShopItem(
                    resultRow[PlayerShopTable.id].value,
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


    fun updatePlayerBalance(player: Player, newBalance: Int) {
        transaction(db) {
            CurrencyBalanceTable.upsert(body = {
                it[uuid] = player.stringUUID
                it[balance] = newBalance
            }, where = { CurrencyBalanceTable.uuid eq player.stringUUID })
        }
    }

    fun getBalance(uuid: String): Int {
        return transaction(db) {
            return@transaction CurrencyBalanceTable.selectAll().where { CurrencyBalanceTable.uuid eq uuid }
                .firstOrNull()?.get(
                    CurrencyBalanceTable.balance
                ) ?: 0
        }
    }

    fun updatePlayerBalance(vuuid: String, newBalance: Int) {
        transaction(db) {
            CurrencyBalanceTable.upsert(body = {
                it[uuid] = vuuid
                it[balance] = newBalance
            }, where = { CurrencyBalanceTable.uuid eq vuuid })
        }
    }

    fun decodeStringToItemStack(string: String): ItemStack? {
        return runCatching {
            ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(string)).result().orElse(null)
        }.getOrNull()
    }

    fun getAllCodexItems(uuid: String): List<RecipeCodexPacketEntry> {
        return transaction(db) {
            return@transaction PlayerCraftsTable.selectAll().where {
                (PlayerCraftsTable.playerUuid eq uuid)
            }.map { resultRow ->
                val conf = RecipeCodexConfig.getAll()[resultRow[PlayerCraftsTable.itemId]]
                RecipeCodexPacketEntry(
                    resultRow[PlayerCraftsTable.itemId],
                    conf?.max ?: 100,
                    resultRow[PlayerCraftsTable.count],
                    conf?.cap ?: 50
                )
            }
        }
    }

    fun getFullCodex(server: MinecraftServer, uuid: String): List<RecipeCodexPacketEntry> {
        val config = RecipeCodexConfig.getAll()

        return RecipeCodexBuilder.build(server, getAllCodexItems(uuid)) { key ->
            RecipeCodexPacketEntry(
                key, config[key]?.max ?: 100, 0, config[key]?.cap ?: 50
            )
        }
    }
}