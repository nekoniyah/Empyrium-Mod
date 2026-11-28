package quest.nekoniyah.empyriummod.database.economy

import net.minecraft.world.entity.player.Player
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase.db

object CurrencyBalanceTable : IntIdTable() {
    val uuid = varchar("uuid", 255)
    val balance = integer("balance").default(0)

    fun updatePlayerBalance(player: Player, newBalance: Int) {
        transaction(db) {
            CurrencyBalanceTable.upsert(body = {
                it[uuid] = player.stringUUID
                it[balance] = newBalance
            }, where = { uuid eq player.stringUUID })
        }
    }

    fun getBalance(uuid: String): Int {
        return transaction(db) {
            return@transaction CurrencyBalanceTable.selectAll().where { CurrencyBalanceTable.uuid eq uuid }
                .firstOrNull()?.get(
                    balance
                ) ?: 0
        }
    }

    fun updatePlayerBalance(vuuid: String, newBalance: Int) {
        transaction(db) {
            CurrencyBalanceTable.upsert(body = {
                it[uuid] = vuuid
                it[balance] = newBalance
            }, where = { uuid eq vuuid })
        }
    }
}