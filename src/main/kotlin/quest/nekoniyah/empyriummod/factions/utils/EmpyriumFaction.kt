package quest.nekoniyah.empyriummod.factions.utils

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.commands.GroupCommand

abstract class EmpyriumFaction : GroupCommand {
    val identifier: String
    val name: MutableComponent
    val description: MutableComponent
    abstract val abilities: List<FactionAbility>
    var factionCommand: FactionCommand

    constructor(identifier: String) : super(identifier) {
        this.identifier = identifier
        this.name = Component.translatable("faction.${EmpyriumMod.MOD_ID}.${this.identifier}.name")
        this.description = Component.translatable("faction.${EmpyriumMod.MOD_ID}.${this.identifier}.description")

        this.factionCommand = FactionCommand(this)
        this.subcommands = factionCommand.subcommands

    }

}