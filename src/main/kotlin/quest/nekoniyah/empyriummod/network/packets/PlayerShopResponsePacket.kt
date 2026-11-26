package quest.nekoniyah.empyriummod.network.packets

import quest.nekoniyah.empyriummod.economy.PlayerShopItem

@JvmRecord
data class PlayerShopResponsePacket(val page: Int, val items: List<PlayerShopItem>)