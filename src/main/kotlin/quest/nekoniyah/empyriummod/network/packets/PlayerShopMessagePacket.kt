package quest.nekoniyah.empyriummod.network.packets

@JvmRecord
data class PlayerShopMessagePacket(val success: Boolean, val message: String)