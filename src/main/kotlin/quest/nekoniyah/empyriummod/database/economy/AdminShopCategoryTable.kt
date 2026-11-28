package quest.nekoniyah.empyriummod.database.economy

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase

object AdminShopCategoryTable : IntIdTable() {
    val name = varchar("name", 255)
    val itemIconKey = varchar("icon_key", 255)
    val position = integer("position").default(1)

    fun createCategory(
        name: String, itemIconKey: String = ItemStack(Items.CHEST).itemHolder.unwrapKey().get().location().toString()
    ): Int? {
        return transaction(EmpyriumDatabase.db) {
            val res = insert {
                it[AdminShopCategoryTable.name] = name
                it[AdminShopCategoryTable.itemIconKey] = itemIconKey
            }.resultedValues?.firstOrNull() ?: return@transaction null

            return@transaction res[AdminShopCategoryTable.id].value
        }
    }

    fun moveCategory(id: Int, newPosition: Int): Boolean {
        getCategory(id) ?: return false

        return transaction(EmpyriumDatabase.db) {
            val res = update(where = {
                AdminShopCategoryTable.id eq id
            }) {
                it[position] = newPosition
            }

            return@transaction res > 0
        }
    }

    fun getCategory(id: Int): CategoryInfo? {
        return transaction(EmpyriumDatabase.db) {
            val r = selectAll().where { AdminShopCategoryTable.id eq id }.firstOrNull() ?: return@transaction null

            return@transaction CategoryInfo(r[name], r[itemIconKey], r[position], r[AdminShopCategoryTable.id].value)
        }
    }

    fun getCategories(): List<CategoryInfo> {
        return transaction(EmpyriumDatabase.db) {
            val res = selectAll().asIterable()

            return@transaction res.map { r ->
                CategoryInfo(r[name], r[itemIconKey], r[position], r[AdminShopCategoryTable.id].value)
            }
        }
    }

    data class CategoryInfo(val name: String, val itemIconKey: String, val position: Int, val id: Int)
}