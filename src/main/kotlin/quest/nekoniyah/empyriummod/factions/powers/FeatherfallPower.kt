package quest.nekoniyah.empyriummod.factions.powers

import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.storage.Ability


class FeatherfallPower : FactionAbility(
    Ability(
        "featherfall",
        displayName = "${EmpyriumMod.MOD_ID}.powers.featherfall.name",
        description = "${EmpyriumMod.MOD_ID}.powers.featherfall.description",
        identifier = "featherfall",
    )
)