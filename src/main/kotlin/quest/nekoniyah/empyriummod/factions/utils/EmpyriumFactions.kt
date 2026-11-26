package quest.nekoniyah.empyriummod.factions.utils

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.EmpyriumMod.NET_CHANNEL
import quest.nekoniyah.empyriummod.commands.SubcommandHolder
import quest.nekoniyah.empyriummod.events.FactionPowerEvents
import quest.nekoniyah.empyriummod.factions.AngelFaction
import quest.nekoniyah.empyriummod.factions.FaunidFaction
import quest.nekoniyah.empyriummod.factions.HumanFaction
import quest.nekoniyah.empyriummod.network.packets.OpenScreenPacket
import quest.nekoniyah.empyriummod.network.packets.SelectFactionPacket

class EmpyriumFactions : SubcommandHolder {

    val attachments: FactionAttachments
    val factions: List<EmpyriumFaction>

    constructor(attachments: FactionAttachments) : super("factions") {
        this.attachments = attachments
        this.factions = listOf(HumanFaction(), FaunidFaction(), AngelFaction())
        this.subcommands = mutableListOf(selfJoinCommand).also { it -> it.addAll(factions.map { it.command }) }
    }

    fun applyAttributeAbilities(player: Player, oldFaction: EmpyriumFaction? = null) {
        if (oldFaction != null) {
            for (ability in oldFaction.abilities) {
                if (ability.isAttribute != null && ability.isAttribute) {
                    ability.unregister(player)
                }
            }
        }

        val factionName = player.getAttached(attachments.FACTION)

        if (factionName != null) {
            val faction = this.factions.find { it.identifier == factionName }

            if (faction == null) return

            for (ability in faction.abilities) {
                if (ability.isAttribute != null && ability.isAttribute) {
                    ability.checkAndExecute(
                        player
                    )
                }
            }

        }
    }

    fun register() {
        ServerPlayerEvents.COPY_FROM.register { oldPlayer, newPlayer, alive ->
            if (!alive) {

                val factionName = oldPlayer.getAttached(attachments.FACTION)
                newPlayer.setAttached(attachments.FACTION, factionName)
                applyAttributeAbilities(newPlayer)
            }
        }

        ServerPlayerEvents.JOIN.register { player ->
            EmpyriumMod.NET_CHANNEL.serverHandle(player).send(OpenScreenPacket("faction_selection"))
        }

        FactionPowerEvents.registerAll()

        NET_CHANNEL.registerServerbound(SelectFactionPacket::class.java) { packet, access ->
            val player = access.player
            val oldFactionName = player.getAttached(EmpyriumMod.factions.attachments.FACTION)
            val faction = if (oldFactionName != null) EmpyriumMod.factions.getFaction(oldFactionName) else null
            EmpyriumMod.factions.joinFaction(player, packet.factionName, faction)
        }
    }

    val selfJoinCommand: ArgumentBuilder<CommandSourceStack, *> = Commands.literal("join").then(
        Commands.argument(
            "faction_name", StringArgumentType.string()
        ).executes(::joinFaction).suggests { _, builder ->
            return@suggests builder.also { b ->
                for (faction in factions) {
                    b.suggest(faction.identifier)
                }
            }.buildFuture()
        })

    fun getFaction(identifier: String): EmpyriumFaction? {
        return factions.find { it.identifier == identifier }
    }

    fun joinFaction(player: Player, faction: String, oldFaction: EmpyriumFaction? = null) {
        player.setAttached(attachments.FACTION, faction)
        applyAttributeAbilities(player, oldFaction)
    }

    fun joinFaction(player: Player, faction: EmpyriumFaction, oldFaction: EmpyriumFaction? = null) {
        player.setAttached(attachments.FACTION, faction.identifier)
        applyAttributeAbilities(player, oldFaction)
    }

    fun joinFaction(ctx: CommandContext<CommandSourceStack>): Int {

        val player = ctx.source.player ?: return 0

        val oldFactionIdentifier = player.getAttached(attachments.FACTION)

        val oldFaction = if (oldFactionIdentifier != null) getFaction(oldFactionIdentifier) else null

        val factionIdentifier = StringArgumentType.getString(ctx, "faction_name")

        val faction = getFaction(factionIdentifier)

        if (faction == null) {
            player.sendSystemMessage(Component.literal("$factionIdentifier is not an existing faction!"))
            return 0
        }

        joinFaction(player, faction, oldFaction)
        return 1
    }

    override fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        return 1
    }
}