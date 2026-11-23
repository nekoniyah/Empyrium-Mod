package quest.nekoniyah.empyriummod.factions

import quest.nekoniyah.empyriummod.factions.powers.*
import quest.nekoniyah.empyriummod.factions.utils.EmpyriumFaction
import quest.nekoniyah.empyriummod.factions.utils.FactionAbility

class FaunidFaction : EmpyriumFaction("faunid") {
    override val abilities: List<FactionAbility> = listOf(
        FeatherfallPower(), FragilePower(), WildSpeedPower(), StrengthenedPower(), VitalizedPower()
    )
}