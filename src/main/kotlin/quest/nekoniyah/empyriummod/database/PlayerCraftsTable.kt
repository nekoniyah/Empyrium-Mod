package quest.nekoniyah.empyriummod.database

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.EmpyriumMod.NET_CHANNEL
import quest.nekoniyah.empyriummod.config.RecipeCodexConfig
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase.db
import quest.nekoniyah.empyriummod.network.packets.RecipeCodexPacketEntry
import quest.nekoniyah.empyriummod.network.packets.RecipeCodexResponsePacket
import kotlin.random.Random

object PlayerCraftsTable : IntIdTable() {
    val playerUuid = varchar("player_uuid", 255)
    val itemId = varchar("item_id", 255)
    val count = integer("count").default(0)

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