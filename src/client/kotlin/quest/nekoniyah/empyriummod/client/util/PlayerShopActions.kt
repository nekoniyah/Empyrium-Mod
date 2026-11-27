package quest.nekoniyah.empyriummod.client.util

import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.economy.PlayerShopItem
import quest.nekoniyah.empyriummod.network.packets.BuyPlayerShopItemPacket
import quest.nekoniyah.empyriummod.network.packets.CancelPlayerShopItemPacket
import quest.nekoniyah.empyriummod.network.packets.PlayerShopRequestPacket
import quest.nekoniyah.empyriummod.network.packets.SellPlayerShopItemPacket

object PlayerShopActions {
    fun sell(inventorySlot: Int, count: Int, currency: String, price: Int) {
        EmpyriumMod.NET_CHANNEL.clientHandle().send(SellPlayerShopItemPacket(currency, count, price, inventorySlot))
    }
    fun requestPage(page: Int) {
        EmpyriumMod.NET_CHANNEL.clientHandle().send(PlayerShopRequestPacket(page))
    }

    fun buy(playerShopItem: PlayerShopItem) {
        EmpyriumMod.NET_CHANNEL.clientHandle().send(BuyPlayerShopItemPacket(playerShopItem.id))
    }

    fun cancel(playerShopItem: PlayerShopItem) {
        EmpyriumMod.NET_CHANNEL.clientHandle().send(CancelPlayerShopItemPacket(playerShopItem.id))
    }
}