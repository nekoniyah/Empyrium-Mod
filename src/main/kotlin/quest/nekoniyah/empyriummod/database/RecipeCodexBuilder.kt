package quest.nekoniyah.empyriummod.database

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.EmpyriumMod.NET_CHANNEL
import quest.nekoniyah.empyriummod.config.RecipeCodexConfig
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase.db
import quest.nekoniyah.empyriummod.network.packets.RecipeCodexPacketEntry
import quest.nekoniyah.empyriummod.network.packets.RecipeCodexResponsePacket
import kotlin.random.Random

object RecipeCodexBuilder {
    fun keyOf(item: Item): String = BuiltInRegistries.ITEM.getKey(item).toString()

    fun keyOf(stack: ItemStack): String = keyOf(stack.item)

    fun craftableKeys(server: MinecraftServer): List<String> =
        server.recipeManager.getAllRecipesFor(RecipeType.CRAFTING)
            .asSequence()
            .map { it.value().getResultItem(server.registryAccess()) }
            // Special recipes (map cloning, firework stars, etc.) have no fixed result
            .filterNot { it.isEmpty }.map { keyOf(it) }.distinct().sorted()
            .toList()

    fun build(
        server: MinecraftServer,
        saved: List<RecipeCodexPacketEntry>,
        notStarted: (itemKey: String) -> RecipeCodexPacketEntry
    ): List<RecipeCodexPacketEntry> {
        val savedByKey = saved.associateBy { it.itemKey }
        return craftableKeys(server).map { savedByKey[it] ?: notStarted(it) }
    }

}