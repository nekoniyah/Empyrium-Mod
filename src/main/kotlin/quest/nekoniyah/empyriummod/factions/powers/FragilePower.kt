package quest.nekoniyah.empyriummod.factions.powers

import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.factions.utils.AttributeCalc
import quest.nekoniyah.empyriummod.factions.utils.AttributeCalcType
import quest.nekoniyah.empyriummod.factions.utils.FactionAbility

class FragilePower : FactionAbility(
    displayName = "${EmpyriumMod.MOD_ID}.powers.fragile.name",
    description = "${EmpyriumMod.MOD_ID}.powers.fragile.description",
    identifier = "fragile",
    mixin = "hurt",
    damageTags = listOf(
        DamageTypes.STARVE.location().toLanguageKey(),
        DamageTypes.DROWN.location().toLanguageKey(),
        DamageTypes.ON_FIRE.location().toLanguageKey(),
        DamageTypes.IN_FIRE.location().toLanguageKey(),
        DamageTypes.FREEZE.location().toLanguageKey(),
        DamageTypes.WITHER.location().toLanguageKey(),
        DamageTypes.MAGIC.location().toLanguageKey(),
        DamageTypes.INDIRECT_MAGIC.location().toLanguageKey()
    )
) {
    override fun execute(player: Player): AttributeCalc {
        return AttributeCalc(AttributeCalcType.MULTIPLY, 0.2f)
    }
}