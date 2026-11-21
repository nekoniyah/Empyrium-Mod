package quest.nekoniyah.empyriummod.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class SelectFactionPayload(val name: String) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<SelectFactionPayload> = TYPE

    companion object : EmpyriumPayloadHolder<SelectFactionPayload>("select_faction") {
        override val CODEC: StreamCodec<FriendlyByteBuf, SelectFactionPayload> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SelectFactionPayload::name, ::SelectFactionPayload
        )
    }

}