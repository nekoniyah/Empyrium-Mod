package quest.nekoniyah.empyriummod.factions.powers

import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.storage.Ability

class ElytraFlightPower : FactionAbility(
    Ability(
        "elytra_flight",
        displayName = "${EmpyriumMod.MOD_ID}.powers.elytra_flight.name",
        description = "${EmpyriumMod.MOD_ID}.powers.elytra_flight.description",
        identifier = "elytra_flight",
        mixin = "fallflying"
    )
)