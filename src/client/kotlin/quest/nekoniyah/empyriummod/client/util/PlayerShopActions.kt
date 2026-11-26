package quest.nekoniyah.empyriummod.client.util

import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.client.screens.ShopResult
import quest.nekoniyah.empyriummod.economy.PlayerShopItem
import quest.nekoniyah.empyriummod.network.packets.PlayerShopRequestPacket

object PlayerShopActions {
    fun sell(inventorySlot: Int, count: Int, currency: String, price: Int, cb: (result: ShopResult) -> Unit) {}
    fun requestPage(page: Int) {
        EmpyriumMod.NET_CHANNEL.clientHandle().send(PlayerShopRequestPacket(page))
    }

    fun buy(playerShopItem: PlayerShopItem, cb: (result: ShopResult) -> Unit) {
        cb(ShopResult(false, "WIP"))
    }

    fun cancel(playerShopItem: PlayerShopItem, cb: (result: ShopResult) -> Unit) {
        cb(ShopResult(false, "WIP"))
    }
}