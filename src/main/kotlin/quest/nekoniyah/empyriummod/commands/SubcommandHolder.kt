package quest.nekoniyah.empyriummod.commands

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

abstract class SubcommandHolder(val literal: String, val needAdmin: Boolean = false) {

    open var subcommands: List<ArgumentBuilder<CommandSourceStack, *>> = listOf()
    abstract fun execute(ctx: CommandContext<CommandSourceStack>): Int

    fun command(): LiteralArgumentBuilder<CommandSourceStack?>? {
        return Commands.literal(literal).executes(::execute).also {
            for (subcommand in subcommands) it.then(subcommand)
            if (needAdmin) it.requires { commandSourceStack -> commandSourceStack.hasPermission(4) }
        }
    }
}