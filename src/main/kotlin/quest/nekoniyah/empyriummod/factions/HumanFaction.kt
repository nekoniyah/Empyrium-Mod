package quest.nekoniyah.empyriummod.factions

import quest.nekoniyah.empyriummod.factions.powers.LaborPower
import quest.nekoniyah.empyriummod.factions.powers.RecipeCodexPower
import quest.nekoniyah.empyriummod.factions.powers.ResistPower
import quest.nekoniyah.empyriummod.factions.utils.EmpyriumFaction
import quest.nekoniyah.empyriummod.factions.utils.FactionAbility

class HumanFaction : EmpyriumFaction("human") {
    override val abilities: List<FactionAbility> = listOf(LaborPower(), ResistPower(), RecipeCodexPower())
    
}