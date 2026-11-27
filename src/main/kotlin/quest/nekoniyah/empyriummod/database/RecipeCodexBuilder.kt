package quest.nekoniyah.empyriummod.database

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import quest.nekoniyah.empyriummod.network.packets.RecipeCodexPacketEntry

object RecipeCodexBuilder {
    fun keyOf(item: Item): String = BuiltInRegistries.ITEM.getKey(item).toString()

    fun keyOf(stack: ItemStack): String = keyOf(stack.item)

    fun craftableKeys(server: MinecraftServer): List<String> =
        server.recipeManager.getAllRecipesFor(RecipeType.CRAFTING)
            .map { it.value().getResultItem(server.registryAccess()) }
            // Special recipes (map cloning, firework stars, etc.) have no fixed result
            .filterNot { it.isEmpty }.map { keyOf(it) }.distinct().sorted()

    fun build(
        server: MinecraftServer,
        saved: List<RecipeCodexPacketEntry>,
        notStarted: (itemKey: String) -> RecipeCodexPacketEntry
    ): List<RecipeCodexPacketEntry> {
        val savedByKey = saved.associateBy { it.itemKey }
        return craftableKeys(server).map { savedByKey[it] ?: notStarted(it) }
    }
}