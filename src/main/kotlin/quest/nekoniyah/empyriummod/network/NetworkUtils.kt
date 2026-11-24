package quest.nekoniyah.empyriummod.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

object NetworkUtils {
    fun <T : CustomPacketPayload> registerPayload(payload: EmpyriumPayloadHolder<T>) {
        PayloadTypeRegistry.configurationS2C().register(payload.TYPE, payload.CODEC)
        PayloadTypeRegistry.playS2C().register(payload.TYPE, payload.CODEC)
    }

    fun <T : CustomPacketPayload> registerPayloadC2S(payload: EmpyriumPayloadHolder<T>) {
        PayloadTypeRegistry.configurationC2S().register(payload.TYPE, payload.CODEC)
        PayloadTypeRegistry.playC2S().register(payload.TYPE, payload.CODEC)
    }


    fun <T : CustomPacketPayload> onClientMessage(
        type: CustomPacketPayload.Type<T>, block: (T, ServerPlayNetworking.Context) -> Unit
    ) {
        ServerPlayNetworking.registerGlobalReceiver(type) { payload, context ->
            block(payload, context)
        }
    }
}