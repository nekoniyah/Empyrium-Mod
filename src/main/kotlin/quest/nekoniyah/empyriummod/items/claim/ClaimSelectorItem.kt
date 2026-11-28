package quest.nekoniyah.empyriummod.items.claim

import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import quest.nekoniyah.empyriummod.EmpyriumMod

class ClaimSelectorItem(properties: Item.Properties) : Item(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {

        val level = context.level
        if (!level.isClientSide) {
            val pos = context.clickedPos
            EmpyriumMod.LOGGER.info("Clic droit sur le bloc en $pos !")
        }

        return InteractionResult.SUCCESS


        //return super.useOn(useOnContext)
    }
}