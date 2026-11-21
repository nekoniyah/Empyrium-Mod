package quest.nekoniyah.empyriummod.factions.powers

import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.storage.Ability

class LaborPower : FactionAbility(
    Ability(
        "labor",
        displayName = "${EmpyriumMod.MOD_ID}.powers.labor.name",
        description = "${EmpyriumMod.MOD_ID}.powers.labor.description",
        identifier = "labor",
        isAttribute = true
    )
) {
    override fun execute(player: Player) {
        val attr = player.getAttribute(Attributes.BLOCK_BREAK_SPEED) ?: return

        attr.addOrUpdateTransientModifier(
            AttributeModifier(
                EmpyriumMod.id("labor_bonus"), 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            )
        )
    }

    override fun unregister(player: Player) {
        val attr = player.getAttribute(Attributes.BLOCK_BREAK_SPEED) ?: return
        attr.removeModifier(EmpyriumMod.id("labor_bonus"))
    }
}