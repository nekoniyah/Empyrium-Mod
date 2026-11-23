package quest.nekoniyah.empyriummod.factions.utils

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import quest.nekoniyah.empyriummod.commands.GroupCommand

class FactionCommand : GroupCommand {
    val faction: EmpyriumFaction

    constructor(faction: EmpyriumFaction) : super(faction.identifier) {
        this.faction = faction
        this.subcommands = listOf()
    }

    override fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        return 1
    }

}