package quest.nekoniyah.empyriummod.factions.utils

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.commands.EmpyriumCommand
import quest.nekoniyah.empyriummod.commands.SubcommandHolder

class PlayerFactionCommand : EmpyriumCommand(listOf(), "faction") {

    override val subcommands: List<SubcommandHolder> = listOf(JoinSubcommand())

    class JoinSubcommand : SubcommandHolder("join") {

        override var subcommands: List<ArgumentBuilder<CommandSourceStack, *>> =
            EmpyriumMod.factions.factions.map { faction ->
                Commands.literal(faction.identifier).executes { ctx -> joinFaction(ctx, faction.identifier) }
            }


        fun joinFaction(context: CommandContext<CommandSourceStack>, identifier: String): Int {

            val player = context.source.player ?: return 0

            val attached = player.getAttached(EmpyriumMod.factions.attachments.FACTION)

            if (attached != null) {
                player.sendSystemMessage(Component.literal("You already have chosen a faction!"))
                return 0;
            }

            EmpyriumMod.factions.joinFaction(player, identifier)

            return 1;
        }

        override fun execute(ctx: CommandContext<CommandSourceStack>): Int {
            return 1;
        }

    }
}