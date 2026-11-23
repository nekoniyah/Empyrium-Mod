package quest.nekoniyah.empyriummod.commands

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

abstract class GroupCommand {

    constructor(groupCommandName: String) {
        this.command = Commands.literal(groupCommandName).executes(::execute).also {
            for (sc in subcommands) {
                it.then(sc)
            }
        }
    }


    var command: LiteralArgumentBuilder<CommandSourceStack>
    var subcommands: List<ArgumentBuilder<CommandSourceStack, *>> = listOf()
    open fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        return 1
    }
}