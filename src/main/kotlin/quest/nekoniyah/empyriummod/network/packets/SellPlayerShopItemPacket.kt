package quest.nekoniyah.empyriummod.network.packets

@JvmRecord
data class SellPlayerShopItemPacket(val currency: String, val count: Int, val price: Int, val slot: Int)