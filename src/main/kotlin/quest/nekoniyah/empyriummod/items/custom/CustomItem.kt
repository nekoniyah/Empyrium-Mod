package quest.nekoniyah.empyriummod.items

import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item

abstract class CustomItem(val id: String, val tabs: List<ResourceKey<CreativeModeTab>>?, val props: Properties?) : Item(props)