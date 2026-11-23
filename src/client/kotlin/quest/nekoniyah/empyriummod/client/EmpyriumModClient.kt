package quest.nekoniyah.empyriummod.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import quest.nekoniyah.empyriummod.client.keymappings.EmpyriumKeyMappings
import quest.nekoniyah.empyriummod.client.screens.FactionSelectionScreen
import quest.nekoniyah.empyriummod.client.screens.ScreenRegistry
import quest.nekoniyah.empyriummod.network.EmpyriumPayloadHolder
import quest.nekoniyah.empyriummod.network.KeybindPayload
import quest.nekoniyah.empyriummod.network.OpenScreenPayload

object EmpyriumModClient : ClientModInitializer {
    const val MOD_ID = "empyrium-mod"

    lateinit var keybinding: EmpyriumKeyMappings

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.

        ScreenRegistry[FactionSelectionScreen.NAME] = FactionSelectionScreen()

        keybinding = EmpyriumKeyMappings()

        onServerMessage(OpenScreenPayload.TYPE) { payload, context ->
            val screenName = payload.name
            if (payload.playerUuid != context.player().stringUUID) return@onServerMessage
            val registeredScreen = ScreenRegistry[screenName] ?: return@onServerMessage
            Minecraft.getInstance().setScreen(registeredScreen)
        }

        ClientTickEvents.END_CLIENT_TICK.register { minecraft ->
            while (keybinding.ABILITY1_KEYBIND.consumeClick()) {
                if (minecraft.player == null) return@register;
                ClientPlayNetworking.send(KeybindPayload(1))
            }
        }
    }

    fun <T : CustomPacketPayload> onServerMessage(
        type: CustomPacketPayload.Type<T>, block: (T, ClientPlayNetworking.Context) -> Unit
    ) {
        ClientPlayNetworking.registerGlobalReceiver(type) { payload, context ->
            block(payload, context)
        }
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}