package quest.nekoniyah.empyriummod.factions.powers

import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.storage.Ability

class FragilePower : FactionAbility(
    Ability(
        "fragile",
        displayName = "${EmpyriumMod.MOD_ID}.powers.fragile.name",
        description = "${EmpyriumMod.MOD_ID}.powers.fragile.description",
        identifier = "fragile",
        mixin = "hurt"
    )
) {
    override fun execute(player: Player): AttributeCalc {
        return AttributeCalc(AttributeCalcType.MULTIPLY, 0.2f)
    }
}