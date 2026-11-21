package quest.nekoniyah.empyriummod.factions.powers

import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.storage.Ability

class StrengthenedPower : FactionAbility(
    Ability(
        "strengthened",
        displayName = "${EmpyriumMod.MOD_ID}.powers.strengthened.name",
        description = "${EmpyriumMod.MOD_ID}.powers.strengthened.description",
        identifier = "strengthened",
        isAttribute = true
    )
) {
    override fun execute(player: Player) {
        val movementSpeed = player.getAttribute(Attributes.ATTACK_DAMAGE) ?: return

        movementSpeed.addTransientModifier(
            AttributeModifier(
                EmpyriumMod.id("strengthened_bonus"), 0.7, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            )
        )
    }

    override fun unregister(player: Player) {
        val movementSpeed = player.getAttribute(Attributes.ATTACK_DAMAGE) ?: return
        movementSpeed.removeModifier(EmpyriumMod.id("strengthened_bonus"))
    }

}