package quest.nekoniyah.empyriummod.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class KeybindPayload(val name: Int) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<KeybindPayload> = TYPE

    companion object : EmpyriumPayloadHolder<KeybindPayload>("keybind") {
        override val CODEC: StreamCodec<FriendlyByteBuf, KeybindPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KeybindPayload::name, ::KeybindPayload
        )
    }

}