package quest.nekoniyah.empyriummod.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import quest.nekoniyah.empyriummod.EmpyriumMod


abstract class EmpyriumPayloadHolder<T : CustomPacketPayload>(val id: String) {
    val TYPE: CustomPacketPayload.Type<T> = CustomPacketPayload.Type(EmpyriumMod.id(id))
    abstract val CODEC: StreamCodec<FriendlyByteBuf, T>
}