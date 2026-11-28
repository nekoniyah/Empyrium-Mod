package quest.nekoniyah.empyriummod.database.claim

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.ReferenceOption

object PlotTable : IntIdTable() {
    val capital = reference("capital", CapitalTable.id, onDelete = ReferenceOption.CASCADE)
}