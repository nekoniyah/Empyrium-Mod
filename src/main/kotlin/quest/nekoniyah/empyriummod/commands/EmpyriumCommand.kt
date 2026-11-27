package quest.nekoniyah.empyriummod.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack

open class EmpyriumCommand(open val subcommands: List<SubcommandHolder> = listOf(), val identifier: String? = "empyrium") {

    fun command(): LiteralArgumentBuilder<CommandSourceStack?>? =
        LiteralArgumentBuilder.literal<CommandSourceStack>(identifier).executes(::execute).also {
            subcommands.forEach { sc -> it.then(sc.command()) }
        }

    open fun execute(ctx: CommandContext<CommandSourceStack>):Int {
        return 1
    }
}