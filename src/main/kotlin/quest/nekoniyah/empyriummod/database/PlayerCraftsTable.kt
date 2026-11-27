package quest.nekoniyah.empyriummod.database

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object PlayerCraftsTable : IntIdTable() {
    val playerUuid = varchar("player_uuid", 255)
    val itemId = varchar("item_id", 255)
    val count = integer("count").default(0)
    val freeCount = integer("free_count").default(0)
}