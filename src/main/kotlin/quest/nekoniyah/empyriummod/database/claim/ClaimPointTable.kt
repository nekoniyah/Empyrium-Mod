package quest.nekoniyah.empyriummod.database.claim

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object ClaimPointTable : IntIdTable() {
    val posX = integer("pos_x")
    val posY = integer("pos_y")
    val posZ = integer("pos_z")
    val posDim = varchar("pos_dim", 255)

    val capitals = reference("capitals", CapitalTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val plots = reference("plots", PlotTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
}