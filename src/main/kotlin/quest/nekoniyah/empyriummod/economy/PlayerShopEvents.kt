package quest.nekoniyah.empyriummod.economy

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod.NET_CHANNEL
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase
import quest.nekoniyah.empyriummod.economy.currencies.CurrencyRegistry
import quest.nekoniyah.empyriummod.network.packets.*
import java.util.*

object PlayerShopEvents {
    fun register() {
        ServerPlayerEvents.JOIN.register { player ->
            EmpyriumDatabase.cachePlayer(player)
            sendPlayerShopUpdate(player)
        }

        NET_CHANNEL.registerClientboundDeferred(OpenScreenPacket::class.java)
        NET_CHANNEL.registerClientboundDeferred(PlayerShopMessagePacket::class.java)

        NET_CHANNEL.registerServerbound(PlayerShopRequestPacket::class.java) { packet, source ->
            sendPlayerShopUpdate(source.player)
        }

        NET_CHANNEL.registerServerbound(CancelPlayerShopItemPacket::class.java) { packet, source ->
            val itemListingId = packet.id
            val existingItem = EmpyriumDatabase.getItem(itemListingId) ?: return@registerServerbound

            if (existingItem.sellerUuid != source.player.stringUUID) return@registerServerbound // Damn, they really wanna hack

            val success = EmpyriumDatabase.removeItem(itemListingId)

            if (success) {
                val decoded = EmpyriumDatabase.decodeStringToItemStack(existingItem.item)
                source.player.inventory.add(decoded!!)

                sendMessage(source.player, true, "Item removed from listings.")
                sendPlayerShopUpdate(source.player.server)
            }
        }

        NET_CHANNEL.registerServerbound(SellPlayerShopItemPacket::class.java) { packet, source ->
            val player = source.player
            val itemstack = source.player.inventory.getItem(packet.slot)

            val curr = CurrencyRegistry.find { it.identifier == packet.currency }
            if (itemstack.count != packet.count) return@registerServerbound // I think they tried to spoof the packet :c
            if (curr == null) return@registerServerbound // Same here

            val newItemstack = itemstack.copyWithCount(itemstack.count - packet.count)
            player.inventory.setItem(packet.slot, newItemstack)
            EmpyriumDatabase.sellItem(player, itemstack, curr, packet.price)

            sendMessage(source.player, true, "Put to sale.")
            sendPlayerShopUpdate(source.player.server)
        }

        NET_CHANNEL.registerServerbound(BuyPlayerShopItemPacket::class.java) { packet, source ->
            val itemId = packet.id
            val itemListing = EmpyriumDatabase.getItem(itemId) ?: return@registerServerbound

            val player = source.player
            val balance = EmpyriumDatabase.getBalance(player.stringUUID)

            if (balance < itemListing.price) return@registerServerbound sendMessage(
                player, false, "You can't afford that!"
            )

            val newBalance = balance - itemListing.price

            val sellerUuid = itemListing.sellerUuid

            EmpyriumDatabase.updatePlayerBalance(player, newBalance)

            val sellerBalance = EmpyriumDatabase.getBalance(sellerUuid)

            EmpyriumDatabase.updatePlayerBalance(sellerUuid, sellerBalance + itemListing.price)

            val itemstack = EmpyriumDatabase.decodeStringToItemStack(itemListing.item) ?: error("")

            val seller = player.server.playerList.getPlayer(UUID.fromString(sellerUuid))
            seller?.sendSystemMessage(Component.literal("You sold ${itemListing.count}x $itemstack for ${itemListing.count} ${CurrencyRegistry.find { it.identifier == itemListing.currency }!!.name}"))

            EmpyriumDatabase.removeItem(itemId)
            player.inventory.add(itemstack)
            sendMessage(source.player, true, "Item bought.")

        }
    }

    fun sendPlayerShopUpdate(server: MinecraftServer) {
        val allItems = EmpyriumDatabase.getItems()

        for (player in server.playerList.players) {
            NET_CHANNEL.serverHandle(player).send(PlayerShopResponsePacket(allItems))
        }
    }

    fun sendPlayerShopUpdate(player: Player) {
        val allItems = EmpyriumDatabase.getItems()
        NET_CHANNEL.serverHandle(player).send(PlayerShopResponsePacket(allItems))
    }

    fun sendMessage(player: Player, success: Boolean, message: String) {
        NET_CHANNEL.serverHandle(player).send(PlayerShopMessagePacket(success, message))
    }

}