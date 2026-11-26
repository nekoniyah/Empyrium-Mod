package quest.nekoniyah.empyriummod.economy

@JvmRecord
data class PlayerShopItem(
    val sellerUuid: String,
    val item: String,
    val count: Int,
    val currency: String,
    val price: Int,
    val sellerName: String,
)