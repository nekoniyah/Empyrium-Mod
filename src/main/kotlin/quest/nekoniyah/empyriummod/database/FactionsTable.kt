package quest.nekoniyah.empyriummod.database

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update


data class FactionInfo(
    val name: String,
    val description: String,
    val spawnX: Int?,
    val spawnY: Int?,
    val spawnZ: Int?,
    val spawnDimension: Int?
)

object FactionsTable : IntIdTable() {
    val name = varchar("faction_name", 50)
    val description = varchar("faction_description", 50)
    val spawnX = integer("spawn_x").nullable()
    val spawnY = integer("spawn_y").nullable()
    val spawnZ = integer("spawn_z").nullable()
    val spawnDimension = integer("spawn_dimension").nullable()

    fun getFactionInfo(name: String): FactionInfo? {
        return transaction(EmpyriumDatabase.db) {
            val existing = selectAll().where { FactionsTable.name eq name }.firstOrNull() ?: return@transaction null
            return@transaction FactionInfo(
                existing[FactionsTable.name],
                existing[description],
                existing[spawnX],
                existing[spawnY],
                existing[spawnZ],
                existing[spawnDimension]
            )
        }
    }

    fun setFactionSpawn(name: String, spawnX: Int, spawnY: Int, spawnZ: Int, spawnDimension: Int): Boolean {
        return transaction(EmpyriumDatabase.db) {
            selectAll().where { FactionsTable.name eq name }.firstOrNull() ?: return@transaction false

            return@transaction transaction(EmpyriumDatabase.db) {
                val res = update(where = { FactionsTable.name eq name }) {
                    it[FactionsTable.spawnX] = spawnX
                    it[FactionsTable.spawnY] = spawnY
                    it[FactionsTable.spawnZ] = spawnZ
                    it[FactionsTable.spawnDimension] = spawnDimension
                }

                return@transaction res > 0
            }
        }
    }

    fun register(name: String, description: String): Boolean {
        return transaction(EmpyriumDatabase.db) {
            val existing = selectAll().where { FactionsTable.name eq name }.firstOrNull()

            return@transaction existing == null && transaction(EmpyriumDatabase.db) {
                val res = insert {
                    it[FactionsTable.name] = name
                    it[FactionsTable.description] = description
                }

                return@transaction res.insertedCount > 0
            }
        }
    }
}