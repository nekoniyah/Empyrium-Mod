package quest.nekoniyah.empyriummod.database

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object PlayerCacheTable : IntIdTable() {
    val uuid = varchar("uuid", 255)
    val name = varchar("name", 255)
}