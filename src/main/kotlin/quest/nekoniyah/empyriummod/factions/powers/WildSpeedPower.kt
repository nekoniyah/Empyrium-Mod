package quest.nekoniyah.empyriummod.factions.powers

import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.storage.Ability

class WildSpeedPower : FactionAbility(
    Ability(
        "wild_speed",
        displayName = "${EmpyriumMod.MOD_ID}.powers.wild_speed.name",
        description = "${EmpyriumMod.MOD_ID}.powers.wild_speed.description",
        identifier = "wild_speed",
        isAttribute = true
    )
) {
    override fun execute(player: Player) {
        val movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED) ?: return

        movementSpeed.addOrUpdateTransientModifier(
            AttributeModifier(EmpyriumMod.id("wild_speed_bonus"), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
        )
    }

    override fun unregister(player: Player) {
        player.getAttribute(Attributes.MOVEMENT_SPEED)?.removeModifier(EmpyriumMod.id("wild_speed_bonus"))
    }
}