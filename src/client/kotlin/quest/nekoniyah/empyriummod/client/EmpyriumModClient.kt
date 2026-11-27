package quest.nekoniyah.empyriummod.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.client.events.KeybindEvents
import quest.nekoniyah.empyriummod.client.keymappings.EmpyriumKeyMappings
import quest.nekoniyah.empyriummod.client.overlays.EmpyriumOverlay
import quest.nekoniyah.empyriummod.client.screens.EmpyriumScreen
import quest.nekoniyah.empyriummod.client.screens.FactionSelectionScreen
import quest.nekoniyah.empyriummod.client.screens.PlayerShopScreen
import quest.nekoniyah.empyriummod.client.screens.RecipeCodexScreen
import quest.nekoniyah.empyriummod.client.screens.ShopResult
import quest.nekoniyah.empyriummod.client.util.ui.CurrencyHud
import quest.nekoniyah.empyriummod.economy.PlayerShopItem
import quest.nekoniyah.empyriummod.economy.currencies.CurrencyRegistry
import quest.nekoniyah.empyriummod.network.packets.*

object EmpyriumModClient : ClientModInitializer {
    const val MOD_ID = "empyrium-mod"

    lateinit var keybinding: EmpyriumKeyMappings

    val screenRegistry: ArrayList<EmpyriumScreen> = arrayListOf(FactionSelectionScreen(), PlayerShopScreen(),
        RecipeCodexScreen())
    var cachedPlayerShopItems: List<PlayerShopItem> = listOf()

    var codex: List<RecipeCodexPacketEntry> = listOf()

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.

        keybinding = EmpyriumKeyMappings()

        EmpyriumMod.NET_CHANNEL.registerClientbound(OpenScreenPacket::class.java) { packet, access ->
            val screen = getScreen(packet.screenName) ?: return@registerClientbound
            Minecraft.getInstance().setScreen(screen)
        }

        KeybindEvents.register()

        CurrencyRegistry.forEach { c ->
            EmpyriumOverlay.addCurrencyData(
                CurrencyHud(
                    c.identifier, id("textures/item/${c.identifier}.png"), 0
                )
            )
        }

        EmpyriumMod.NET_CHANNEL.registerClientbound(PlayerShopResponsePacket::class.java) { packet, _ ->
            cachedPlayerShopItems = packet.items
            (Minecraft.getInstance().screen as? PlayerShopScreen)?.refresh()
        }

        EmpyriumMod.NET_CHANNEL.registerClientbound(PlayerShopMessagePacket::class.java) { packet, _ ->
            (Minecraft.getInstance().screen as? PlayerShopScreen)
                ?.onShopResult(ShopResult(packet.success, packet.message))
        }

        EmpyriumMod.NET_CHANNEL.registerClientbound(RecipeCodexResponsePacket::class.java) { packet, _ ->
            codex = packet.items
        }

        ClientTickEvents.END_CLIENT_TICK.register { minecraft ->
            val player = minecraft.player ?: return@register

            CurrencyRegistry.forEach { c ->
                val currencyVal = player.getAttached(c.ATTACHMENT) ?: 0

                EmpyriumOverlay.updateCurrencyData(
                    CurrencyHud(
                        c.identifier, id("textures/item/${c.identifier}.png"), currencyVal
                    )
                )
            }

            EmpyriumOverlay.update()
        }

        EmpyriumOverlay.init()
    }


    fun getScreen(identifier: String): EmpyriumScreen? {
        return screenRegistry.find { it.identifier == identifier }
    }


    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}