package quest.nekoniyah.empyriummod.database.economy

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase

object AdminShopTable : IntIdTable() {
    val itemData = text("item_data")
    val itemCount = integer("item_count")
    val currency = text("currency_identifier")
    val sellPrice = integer("sell_price").nullable()
    val buyPrice = integer("buy_price").nullable()
    val category = reference(
        name = "category",
        refColumn = AdminShopCategoryTable.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    )

    fun removeItem(id: Int): Boolean {
        return transaction(EmpyriumDatabase.db) {
            selectAll().where { AdminShopTable.id eq id }.firstOrNull() ?: return@transaction false

            return@transaction transaction(EmpyriumDatabase.db) {
                val res = deleteWhere { AdminShopTable.id eq id }
                return@transaction res > 0
            }

        }
    }

    fun addItem(data: String, count: Int, currency: String, categoryId: Int, sellPrice: Int? = null, buyPrice: Int? = null): Boolean {
        return transaction(EmpyriumDatabase.db) {
            val res = insert {
                it[AdminShopTable.itemData] = data
                it[AdminShopTable.itemCount] = count
                it[AdminShopTable.currency] = currency
                it[AdminShopTable.sellPrice] = sellPrice
                it[AdminShopTable.buyPrice] = buyPrice
                it[AdminShopTable.category] = categoryId
            }

            return@transaction res.insertedCount > 0
        }
    }
}