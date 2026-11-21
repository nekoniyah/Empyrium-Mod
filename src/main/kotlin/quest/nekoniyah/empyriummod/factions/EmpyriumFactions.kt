package quest.nekoniyah.empyriummod.factions

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.commands.SubcommandHolder
import quest.nekoniyah.empyriummod.factions.powers.*
import quest.nekoniyah.empyriummod.storage.Ability

class EmpyriumFactions(val attachments: FactionAttachments) : SubcommandHolder("factions") {
    val abilities: List<FactionAbility> = listOf(
        FragilePower(),
        WildSpeedPower(),
        VitalizedPower(),
        ElytraFlightPower(),
        StrengthenedPower(),
        ResistPower(),
        LaborPower(),
        LaunchPower(),
        FeatherfallPower()
    )

    fun applyAttributeAbilities(player: Player, oldFactionName: String? = null) {
        if (oldFactionName != null) {
            val faction = EmpyriumMod.storage.getFaction(oldFactionName)

            if (faction != null) {
                for (ability in faction.abilities) {
                    val workingAbility = getWorkingAbility(ability) ?: continue
                    if (workingAbility.data.isAttribute != null && workingAbility.data.isAttribute) {
                        workingAbility.unregister(player)
                    }
                }
            }
        }

        val factionName = player.getAttached(attachments.FACTION)

        if (factionName != null) {
            val factionEntry = EmpyriumMod.storage.getFaction(factionName) ?: return

            for (ability in factionEntry.abilities) {
                val workingAbility = getWorkingAbility(ability) ?: continue
                if (workingAbility.data.isAttribute != null && workingAbility.data.isAttribute) {
                    workingAbility.checkAndExecute(
                        player
                    )
                }
            }

        }
    }

    fun register() {
        val server = EmpyriumMod.server ?: return

        ServerPlayerEvents.COPY_FROM.register { oldPlayer, newPlayer, alive ->
            if (!alive) {

                val factionName = oldPlayer.getAttached(attachments.FACTION)
                newPlayer.setAttached(attachments.FACTION, factionName)
                applyAttributeAbilities(newPlayer)
            }
        }
    }

    val selfJoinCommand: ArgumentBuilder<CommandSourceStack, *> = Commands.literal("join").then(
        Commands.argument(
            "faction_name", StringArgumentType.string()
        ).executes(::joinFaction)
    )

    fun joinFaction(player: Player, factionName: String, oldFactionName: String? = null) {
        player.setAttached(attachments.FACTION, factionName)
        applyAttributeAbilities(player, oldFactionName)
    }

    fun joinFaction(ctx: CommandContext<CommandSourceStack>): Int {

        val player = ctx.source.player ?: return 0

        val oldFactionName = player.getAttached(attachments.FACTION)

        val factionName = StringArgumentType.getString(ctx, "faction_name")

        val factionEntry = EmpyriumMod.storage.getFaction(factionName)

        if (factionEntry == null) {
            player.sendSystemMessage(Component.literal("$factionName is not an existing faction!"))
            return 1
        }

        joinFaction(player, factionName, oldFactionName)
        return 0
    }

    fun getWorkingAbility(ability: Ability): FactionAbility? {
        return abilities.find { it.data.identifier == ability.identifier }
    }

    override fun subcommands(): List<ArgumentBuilder<CommandSourceStack, *>> {
        return listOf(selfJoinCommand)
    }

    override fun execute(source: CommandContext<CommandSourceStack>): Int {
        return 0
    }
}