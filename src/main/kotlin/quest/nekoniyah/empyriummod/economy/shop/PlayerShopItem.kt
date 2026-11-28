package quest.nekoniyah.empyriummod.economy.shop

@JvmRecord
data class PlayerShopItem(
    val id: Int,
    val sellerUuid: String,
    val item: String,
    val count: Int,
    val currency: String,
    val price: Int,
    val sellerName: String,
)