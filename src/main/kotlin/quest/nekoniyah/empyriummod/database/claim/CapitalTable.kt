package quest.nekoniyah.empyriummod.database.claim

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object CapitalTable : IntIdTable() {
    var name = varchar("name", 255)

}