package quest.nekoniyah.empyriummod.economy.currencies

import com.mojang.serialization.Codec
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.network.codec.ByteBufCodecs
import quest.nekoniyah.empyriummod.EmpyriumMod

abstract class Currency(val identifier: String, val name: String, val tier: Int) {
    val ATTACHMENT: AttachmentType<Int> = AttachmentRegistry.create(EmpyriumMod.id("currency.$identifier")) { builder ->
        builder.initializer { 0 }.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()).persistent(Codec.INT)
            .copyOnDeath()
    }
}