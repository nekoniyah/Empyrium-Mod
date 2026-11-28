package quest.nekoniyah.empyriummod.items.claim

import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import quest.nekoniyah.empyriummod.EmpyriumMod

class ClaimSelectorItem(properties: Item.Properties) : Item(properties) {

    private val LOGGER = EmpyriumMod.LOGGER

    companion object {
        // DataComponent 
        val CLAIM_POINTS: DataComponentType<List<BlockPos>> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(EmpyriumMod.MOD_ID, "claim_points"),
            DataComponentType.builder<List<BlockPos>>()
                .networkSynchronized(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()))
                .build()
        )
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level

        if (level.isClientSide) return InteractionResult.PASS

        val pos: BlockPos = context.clickedPos

        val itemInHand: ItemStack = context.itemInHand
        val claimPoints: List<BlockPos> = itemInHand.getOrDefault(CLAIM_POINTS, emptyList())

        if (pos in claimPoints) {
            itemInHand.set(CLAIM_POINTS, claimPoints - pos)
            LOGGER.info("Clic droit sur le bloc en $pos ! Retirer")
            return InteractionResult.CONSUME
        }

        LOGGER.info("Clic droit sur le bloc en $pos ! Ajouté")
        itemInHand.set(CLAIM_POINTS, claimPoints + pos)

        return InteractionResult.SUCCESS

        //return super.useOn(useOnContext)
    }

    override fun isEnchantable(itemStack: ItemStack): Boolean {
        return false
    }

    override fun canAttackBlock(blockState: BlockState, level: Level, blockPos: BlockPos, player: Player): Boolean {
        return false
    }
}