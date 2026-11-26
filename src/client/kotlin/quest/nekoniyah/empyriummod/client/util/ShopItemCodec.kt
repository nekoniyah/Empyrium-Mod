package quest.nekoniyah.empyriummod.client.util

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack

object ShopItemCodec {
    fun decode(json: String): ItemStack? {
        val level = Minecraft.getInstance().level ?: return null
        val ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE)

        return runCatching {
            ItemStack.CODEC.parse(ops, JsonParser.parseString(json)).result().orElse(null)
        }.getOrNull()
    }
}