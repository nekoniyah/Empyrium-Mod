package quest.nekoniyah.empyriummod.client.events

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import quest.nekoniyah.empyriummod.client.EmpyriumModClient.getScreen
import quest.nekoniyah.empyriummod.network.OpenScreenPayload

object NetworkEvents {
    fun register() {
        onServerMessage(OpenScreenPayload.TYPE) { payload, context ->
            val screenName = payload.name
            val registeredScreen = getScreen(screenName) ?: return@onServerMessage
            Minecraft.getInstance().setScreen(registeredScreen)
        }

    }

    fun <T : CustomPacketPayload> onServerMessage(
        type: CustomPacketPayload.Type<T>, block: (T, ClientPlayNetworking.Context) -> Unit
    ) {
        ClientPlayNetworking.registerGlobalReceiver(type) { payload, context ->
            block(payload, context)
        }
    }
}