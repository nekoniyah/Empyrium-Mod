package quest.nekoniyah.empyriummod.database

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object FactionsTable : IntIdTable() {
    val name = varchar("faction_name", 50)
    val description = varchar("faction_description", 50)
    val spawnX = integer("spawn_x")
    val spawnY = integer("spawn_y")
    val spawnZ = integer("spawn_z")
    val spawnDimension = integer("spawn_dimension")
}