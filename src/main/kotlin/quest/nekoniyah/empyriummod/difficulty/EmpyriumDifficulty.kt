package quest.nekoniyah.empyriummod.difficulty

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.*
import net.minecraft.server.level.ServerPlayer
import quest.nekoniyah.empyriummod.commands.SubcommandHolder

class EmpyriumDifficulty(val attachments: DifficultyAttachments) : SubcommandHolder("difficulty") {
    val dict = HashMap<String, String>()

    init {
        dict["normal"] = "Base Vanilla Behavior. Inventory and XP is lost on death."
        dict["safe"] = "Inventory and XP is preserved on death."
    }

    override fun execute(source: CommandContext<CommandSourceStack>): Int {
        return 0
    }

    val selectDifficultyCommand: ArgumentBuilder<CommandSourceStack, *> =
        Commands.argument("name", StringArgumentType.string()).executes(::defineSelfDifficulty).then(
            Commands.argument("player", EntityArgument.player()).executes(::definePlayerDifficulty)
        )


    override var subcommands = listOf(selectDifficultyCommand)

    fun defineSelfDifficulty(ctx: CommandContext<CommandSourceStack>): Int {
        val name = StringArgumentType.getString(ctx, "name")

        if (!dict.keys.contains(name)) {
            ctx.source.sendSystemMessage(Component.literal("Difficulty $name doesn't exist."))
            return 0
        }


        ctx.source.player?.setAttached(attachments.DIFFICULTY, name)
        val playerName: String = ctx.source.player?.name?.string.toString()

        ctx.source.sendSystemMessage(Component.literal("Set difficulty $name to $playerName."))
        return 1
    }

    fun definePlayerDifficulty(ctx: CommandContext<CommandSourceStack>): Int {
        val player = EntityArgument.getPlayer(ctx, "player")
        val name = StringArgumentType.getString(ctx, "name")

        val playerName: String

        if (ctx.source.hasPermission(4)) {
            if (!dict.keys.contains(name)) {
                ctx.source.sendSystemMessage(Component.literal("Difficulty $name does not exist."))
                return 0
            }

            player.setAttached(attachments.DIFFICULTY, name)
            playerName = player.name.string
        } else {
            ctx.source.sendSystemMessage(Component.literal("You don't have the permission to execute this command."))
            return 0
        }

        ctx.source.sendSystemMessage(Component.literal("Set difficulty $name to $playerName."))

        return 1
    }

    fun button(text: String, difficulty: String): MutableComponent {
        return Component.empty().append(Component.literal("[").withStyle(ChatFormatting.RED))
            .append(Component.literal(text).withStyle(ChatFormatting.GOLD))
            .append(Component.literal("]").withStyle(ChatFormatting.RED)).withStyle(
                Style.EMPTY.withClickEvent(
                    ClickEvent(
                        ClickEvent.Action.RUN_COMMAND, "/empyrium difficulty " + difficulty
                    )
                ).withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(dict[difficulty]!!)))
            )

    }

    fun onPlayerJoin(player: ServerPlayer) {
        val playerDifficulty = player.getAttached(attachments.DIFFICULTY)

        if (playerDifficulty.equals("none") || playerDifficulty == null) {
            val component =
                Component.empty().append("Choose a difficulty\n").append(button("Safe", "safe")).append(" or ")
                    .append(button("Normal", "normal"))

            player.sendSystemMessage(component)
        }
    }

    fun register(): EmpyriumDifficulty {
        ServerPlayerEvents.JOIN.register { player -> onPlayerJoin(player) }
        ServerPlayerEvents.COPY_FROM.register { oldPlayer, newPlayer, alive ->
            if (!alive) {
                val difficulty = oldPlayer.getAttached(attachments.DIFFICULTY) ?: return@register

                newPlayer.setAttached(attachments.DIFFICULTY, difficulty)

                if (difficulty == "safe") {
                    newPlayer.inventory.replaceWith(oldPlayer.inventory)
                    newPlayer.experienceProgress = oldPlayer.experienceProgress
                    newPlayer.score = oldPlayer.score
                    newPlayer.experienceLevel = oldPlayer.experienceLevel
                    newPlayer.totalExperience = oldPlayer.totalExperience
                }
            }
        }

        return this
    }
}