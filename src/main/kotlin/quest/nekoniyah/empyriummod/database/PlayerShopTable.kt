package quest.nekoniyah.empyriummod.database

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object PlayerShopTable : IntIdTable() {
    val itemData = text("item_data")
    val itemCount = integer("item_count")
    val sellerUuid = varchar("seller_uuid", 255)
    val currency = text("currency_identifier")
    val price = integer("price")
}