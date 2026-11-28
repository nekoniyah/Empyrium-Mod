package quest.nekoniyah.empyriummod.network.packets

import quest.nekoniyah.empyriummod.economy.shop.PlayerShopItem

@JvmRecord
data class PlayerShopResponsePacket(val items: List<PlayerShopItem>)