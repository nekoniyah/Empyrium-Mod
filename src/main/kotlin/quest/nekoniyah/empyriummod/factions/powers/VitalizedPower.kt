package quest.nekoniyah.empyriummod.factions.powers

import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.storage.Ability

class VitalizedPower : FactionAbility(
    Ability(
        "vitalized",
        displayName = "${EmpyriumMod.MOD_ID}.powers.vitalized.name",
        description = "${EmpyriumMod.MOD_ID}.powers.vitalized.description",
        identifier = "vitalized",
        isAttribute = true
    )
) {
    override fun execute(player: Player) {
        val movementSpeed = player.getAttribute(Attributes.MAX_HEALTH) ?: return

        movementSpeed.addOrUpdateTransientModifier(
            AttributeModifier(EmpyriumMod.id("vitalized_bonus"), 8.0, AttributeModifier.Operation.ADD_VALUE)
        )
    }

    override fun unregister(player: Player) {
        val movementSpeed = player.getAttribute(Attributes.MAX_HEALTH) ?: return
        movementSpeed.removeModifier(EmpyriumMod.id("vitalized_bonus"))
    }
}