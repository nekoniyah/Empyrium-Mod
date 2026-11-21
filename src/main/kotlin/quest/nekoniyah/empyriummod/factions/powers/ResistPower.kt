package quest.nekoniyah.empyriummod.factions.powers

import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.storage.Ability

class ResistPower : FactionAbility(
    Ability(
        "resist",
        displayName = "${EmpyriumMod.MOD_ID}.powers.resist.name",
        description = "${EmpyriumMod.MOD_ID}.powers.resist.description",
        identifier = "resist",
        mixin = "hurt"
    )
) {
    override fun execute(player: Player): AttributeCalc {
        return AttributeCalc(AttributeCalcType.MULTIPLY, -0.2f)
    }
}