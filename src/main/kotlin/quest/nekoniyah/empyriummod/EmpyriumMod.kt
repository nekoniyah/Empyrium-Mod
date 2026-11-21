package quest.nekoniyah.empyriummod

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import quest.nekoniyah.empyriummod.commands.EmpyriumCommand
import quest.nekoniyah.empyriummod.difficulty.DifficultyAttachments
import quest.nekoniyah.empyriummod.difficulty.EmpyriumDifficulty
import quest.nekoniyah.empyriummod.factions.EmpyriumFactions
import quest.nekoniyah.empyriummod.factions.FactionAttachments
import quest.nekoniyah.empyriummod.factions.powers.AbilityExecutionResult
import quest.nekoniyah.empyriummod.factions.powers.PowerCooldowns
import quest.nekoniyah.empyriummod.network.EmpyriumPayloadHolder
import quest.nekoniyah.empyriummod.network.KeybindPayload
import quest.nekoniyah.empyriummod.network.OpenScreenPayload
import quest.nekoniyah.empyriummod.network.SelectFactionPayload
import quest.nekoniyah.empyriummod.storage.StorageManager

object EmpyriumMod : ModInitializer {
    const val MOD_ID: String = "empyrium-mod"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    val difficulty = EmpyriumDifficulty(DifficultyAttachments())
    val factions = EmpyriumFactions(FactionAttachments())

    var server: MinecraftServer? = null
    lateinit var storage: StorageManager

    fun <T : CustomPacketPayload> registerPayload(payload: EmpyriumPayloadHolder<T>) {
        PayloadTypeRegistry.configurationS2C().register(payload.TYPE, payload.CODEC)
        PayloadTypeRegistry.playS2C().register(payload.TYPE, payload.CODEC)
    }

    fun <T : CustomPacketPayload> registerPayloadC2S(payload: EmpyriumPayloadHolder<T>) {
        PayloadTypeRegistry.configurationC2S().register(payload.TYPE, payload.CODEC)
        PayloadTypeRegistry.playC2S().register(payload.TYPE, payload.CODEC)
    }

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        registerPayload(OpenScreenPayload)
        registerPayloadC2S(OpenScreenPayload)
        registerPayloadC2S(SelectFactionPayload)
        registerPayload(SelectFactionPayload)

        registerPayloadC2S(KeybindPayload)

        storage = StorageManager()

        ServerWorldEvents.LOAD.register { srv, _ ->
            if (server == null) {
                server = srv
                factions.register()
            }
        }

        ModItems.initialize()
        difficulty.register()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(EmpyriumCommand(listOf(factions, difficulty, storage)).command)
        }

        LOGGER.info("Hello Fabric world!")

        ServerPlayerEvents.JOIN.register { player ->
            ServerPlayNetworking.send(player, OpenScreenPayload("faction_selection", player.stringUUID))
        }

        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, f ->
            if (entity !is ServerPlayer) return@register true

            val factionName = entity.getAttached(factions.attachments.FACTION) ?: return@register true
            val faction = storage.getFaction(factionName) ?: return@register true

            val featherfallAbility = faction.abilities.find { it.identifier == "featherfall" } ?: return@register true

            val workingAbility = factions.getWorkingAbility(featherfallAbility) ?: return@register true
            if (workingAbility.data.name != "featherfall") return@register true

            if(!source.`is`(DamageTypeTags.IS_FALL)) return@register true

            return@register false
        }

        ServerTickEvents.END_SERVER_TICK.register { server ->
            val players = server.playerList.players

            for (player in players) {
                val factionName = player.getAttached(factions.attachments.FACTION) ?: continue
                val faction = storage.getFaction(factionName) ?: continue

                for (ability in faction.abilities) {
                    val workingAbility = factions.getWorkingAbility(ability) ?: continue
                    if (workingAbility.data.passive) continue

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

        onClientMessage(SelectFactionPayload.TYPE) { payload, context ->
            factions.joinFaction(context.player(), payload.name)
        }

        onClientMessage(KeybindPayload.TYPE) { payload, context ->
            val keybindId = payload.name
            val player = context.player()

            val factionName = player.getAttached(factions.attachments.FACTION) ?: return@onClientMessage
            val faction = storage.getFaction(factionName) ?: return@onClientMessage

            for (ability in faction.abilities) {
                val workingAbility = factions.getWorkingAbility(ability) ?: continue

                if (workingAbility.data.keybind != null && workingAbility.data.keybind == keybindId) {
                    val res = workingAbility.checkAndExecute(player)

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


    fun <T : CustomPacketPayload> onClientMessage(
        type: CustomPacketPayload.Type<T>, block: (T, ServerPlayNetworking.Context) -> Unit
    ) {
        ServerPlayNetworking.registerGlobalReceiver(type) { payload, context ->
            block(payload, context)
        }
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

}
