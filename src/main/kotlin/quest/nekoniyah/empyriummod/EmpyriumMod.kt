package quest.nekoniyah.empyriummod

import com.llamalad7.mixinextras.MixinExtrasBootstrap
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import quest.nekoniyah.empyriummod.commands.EmpyriumCommand
import quest.nekoniyah.empyriummod.difficulty.DifficultyAttachments
import quest.nekoniyah.empyriummod.difficulty.EmpyriumDifficulty
import quest.nekoniyah.empyriummod.events.FactionPowerEvents
import quest.nekoniyah.empyriummod.factions.utils.EmpyriumFactions
import quest.nekoniyah.empyriummod.factions.utils.FactionAttachments
import quest.nekoniyah.empyriummod.network.EmpyriumPayloadHolder
import quest.nekoniyah.empyriummod.network.KeybindPayload
import quest.nekoniyah.empyriummod.network.OpenScreenPayload
import quest.nekoniyah.empyriummod.network.SelectFactionPayload

object EmpyriumMod : ModInitializer {
    const val MOD_ID: String = "empyrium-mod"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    val difficulty = EmpyriumDifficulty(DifficultyAttachments())
    val factions = EmpyriumFactions(FactionAttachments())

    var server: MinecraftServer? = null

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

        MixinExtrasBootstrap.init()

        registerPayload(OpenScreenPayload)
        registerPayloadC2S(SelectFactionPayload)

        registerPayloadC2S(KeybindPayload)


        ServerLifecycleEvents.SERVER_STARTED.register { srv ->
            server = srv
            factions.register()
        }

        ModItems.initialize()
        difficulty.register()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(EmpyriumCommand(listOf(factions, difficulty)).command)
        }


        LOGGER.info("Hello Fabric world!")
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
