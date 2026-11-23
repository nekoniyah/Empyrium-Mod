package quest.nekoniyah.empyriummod.factions

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import quest.nekoniyah.empyriummod.factions.powers.*
import quest.nekoniyah.empyriummod.factions.utils.EmpyriumFaction
import quest.nekoniyah.empyriummod.factions.utils.FactionAbility

class AngelFaction : EmpyriumFaction("angel") {
    override val abilities: List<FactionAbility> = listOf(
        FeatherfallPower(), FragilePower(), LaunchPower(), ElytraFlightPower(), VitalizedPower()
    )
}