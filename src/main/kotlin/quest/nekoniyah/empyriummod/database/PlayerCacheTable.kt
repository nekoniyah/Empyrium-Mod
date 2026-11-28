package quest.nekoniyah.empyriummod.database

import net.minecraft.world.entity.player.Player
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase.db

object PlayerCacheTable : IntIdTable() {
    val uuid = varchar("uuid", 255)
    val name = varchar("name", 255)

    fun getCachedPlayerName(uuid: String): String? {
        return transaction(db) {
            val alreadyCached = PlayerCacheTable.selectAll().where {
                PlayerCacheTable.uuid eq uuid
            }.firstOrNull()

            return@transaction alreadyCached?.get(name)
        }
    }

    fun cachePlayer(player: Player) {
        transaction(db) {
            PlayerCacheTable.upsert(body = {
                it[uuid] = player.stringUUID
                it[name] = player.name.string
            }, where = {
                uuid eq player.stringUUID
            })
        }
    }
}