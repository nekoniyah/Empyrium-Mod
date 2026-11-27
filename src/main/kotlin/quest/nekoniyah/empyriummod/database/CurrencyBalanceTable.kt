package quest.nekoniyah.empyriummod.database

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object CurrencyBalanceTable : IntIdTable() {
    val uuid = varchar("uuid", 255)
    val balance = integer("balance").default(0)
}