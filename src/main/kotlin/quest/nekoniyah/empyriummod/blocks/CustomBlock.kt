package quest.nekoniyah.empyriummod.blocks

import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour

abstract class CustomBlock(val id: String, val tabs: List<ResourceKey<CreativeModeTab>>?, val props: BlockBehaviour.Properties?) : Block(props)