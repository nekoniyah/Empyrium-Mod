package quest.nekoniyah.empyriummod.items

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.items.claim.ClaimSelectorItem

object ModItems {

    val registeredItems = listOf(TestItem)

    val CLAIM_SELECTOR: Item = register(ClaimSelectorItem(Item.Properties().stacksTo(1)), "claim_selector")


    fun register(item: Item, id: String): Item {
        // Create the identifier for the item.
        val itemID = ResourceLocation.fromNamespaceAndPath(EmpyriumMod.MOD_ID, id)
        // Register the item.
        val registeredItem = Registry.register(BuiltInRegistries.ITEM, itemID, item)
        // Return the registered item!
        return registeredItem
    }


    fun listenItemGroupEventWith(key: ResourceKey<CreativeModeTab>) {
        ItemGroupEvents.modifyEntriesEvent(key).register { e ->
            for (item in registeredItems) {
                if (item.tabs == null) continue
                if ((item.tabs as Iterable<Any?>).contains(key)) e.accept(item.item)
            }
        }
    }

    fun initialize() {
        listenItemGroupEventWith(CreativeModeTabs.BUILDING_BLOCKS)
        listenItemGroupEventWith(CreativeModeTabs.INGREDIENTS)
        listenItemGroupEventWith(CreativeModeTabs.COLORED_BLOCKS)
        listenItemGroupEventWith(CreativeModeTabs.COMBAT)
        listenItemGroupEventWith(CreativeModeTabs.FOOD_AND_DRINKS)
        listenItemGroupEventWith(CreativeModeTabs.FUNCTIONAL_BLOCKS)
        listenItemGroupEventWith(CreativeModeTabs.NATURAL_BLOCKS)
        listenItemGroupEventWith(CreativeModeTabs.REDSTONE_BLOCKS)
        listenItemGroupEventWith(CreativeModeTabs.SPAWN_EGGS)
        listenItemGroupEventWith(CreativeModeTabs.TOOLS_AND_UTILITIES)
        listenItemGroupEventWith(CreativeModeTabs.OP_BLOCKS)
    }
}