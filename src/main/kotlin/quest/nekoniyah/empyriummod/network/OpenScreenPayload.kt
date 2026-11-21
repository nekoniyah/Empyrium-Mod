package quest.nekoniyah.empyriummod.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class OpenScreenPayload(val name: String, val playerUuid: String) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<OpenScreenPayload> = TYPE

    companion object : EmpyriumPayloadHolder<OpenScreenPayload>("open_screen") {
        override val CODEC: StreamCodec<FriendlyByteBuf, OpenScreenPayload> =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, OpenScreenPayload::name,
                ByteBufCodecs.STRING_UTF8, OpenScreenPayload::playerUuid,
                ::OpenScreenPayload
            )
    }
}