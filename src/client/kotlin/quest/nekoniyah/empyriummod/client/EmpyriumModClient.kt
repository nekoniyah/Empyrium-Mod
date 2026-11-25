package quest.nekoniyah.empyriummod.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.resources.ResourceLocation
import quest.nekoniyah.empyriummod.client.events.KeybindEvents
import quest.nekoniyah.empyriummod.client.events.NetworkEvents
import quest.nekoniyah.empyriummod.client.keymappings.EmpyriumKeyMappings
import quest.nekoniyah.empyriummod.client.overlays.EmpyriumOverlay
import quest.nekoniyah.empyriummod.client.screens.EmpyriumScreen
import quest.nekoniyah.empyriummod.client.screens.FactionSelectionScreen
import quest.nekoniyah.empyriummod.client.util.ui.CurrencyHud
import quest.nekoniyah.empyriummod.economy.currencies.CurrencyRegistry

object EmpyriumModClient : ClientModInitializer {
    const val MOD_ID = "empyrium-mod"

    lateinit var keybinding: EmpyriumKeyMappings

    val screenRegistry: ArrayList<EmpyriumScreen> = arrayListOf(FactionSelectionScreen())

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.


        keybinding = EmpyriumKeyMappings()

        NetworkEvents.register()
        KeybindEvents.register()

        CurrencyRegistry.forEach { c ->

            EmpyriumOverlay.addCurrencyData(
                CurrencyHud(
                    c.identifier, id("textures/item/${c.identifier}.png"), 0
                )
            )
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