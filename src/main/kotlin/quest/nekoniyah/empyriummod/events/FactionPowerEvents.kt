package quest.nekoniyah.empyriummod.events

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags
import quest.nekoniyah.empyriummod.EmpyriumMod.NET_CHANNEL
import quest.nekoniyah.empyriummod.EmpyriumMod.factions
import quest.nekoniyah.empyriummod.factions.utils.AbilityExecutionResult
import quest.nekoniyah.empyriummod.factions.utils.PowerCooldowns
import quest.nekoniyah.empyriummod.network.packets.KeybindPacket

object FactionPowerEvents {
    fun registerNetworkEvents() {
        NET_CHANNEL.registerServerbound(KeybindPacket::class.java) { packet, access ->
            val keybindId = packet.name
            val player = access.player()

            val factionName = player.getAttached(factions.attachments.FACTION) ?: return@registerServerbound
            val faction = factions.getFaction(factionName) ?: return@registerServerbound

            for (ability in faction.abilities) {
                if (ability.keybind != null && ability.keybind == keybindId) {
                    val res = ability.checkAndExecute(player)

                    val message = when (res) {
                        AbilityExecutionResult.DENIED -> "You are under cooldown!"
                        else -> null
                    }

                    if (message != null) {
                        player.sendSystemMessage(Component.literal(message))
                    }

                    break;
                }
            }
        }
    }

    fun registerServerTick() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val players = server.playerList.players

            for (player in players) {
                val factionName = player.getAttached(factions.attachments.FACTION) ?: continue
                val faction = factions.getFaction(factionName) ?: continue

                for (ability in faction.abilities) {
                    if (ability.passive) continue

                    val powerCooldown =
                        PowerCooldowns.find { it.abilityIdentifier == ability.identifier && it.player.stringUUID == player.stringUUID }
                            ?: continue
                    powerCooldown.cooldown -= 1

                    if (powerCooldown.cooldown <= 0) {
                        PowerCooldowns.remove(powerCooldown)
                    }

                }
            }
        }
    }

    fun registerServerEntityDamage() {

        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, f ->
            if (entity !is ServerPlayer) return@register true

            val factionName = entity.getAttached(factions.attachments.FACTION) ?: return@register true
            val faction = factions.getFaction(factionName) ?: return@register true

            val featherfallAbility = faction.abilities.find { it.identifier == "featherfall" } ?: return@register true

            if (featherfallAbility.identifier != "featherfall") return@register true

            if (!source.`is`(DamageTypeTags.IS_FALL)) return@register true

            return@register false
        }
    }

    fun registerAll() {
        this.registerNetworkEvents()
        this.registerServerTick()
        this.registerServerEntityDamage()
    }
}