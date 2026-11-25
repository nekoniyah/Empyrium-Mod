package quest.nekoniyah.empyriummod

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import quest.nekoniyah.empyriummod.commands.EmpyriumCommand
import quest.nekoniyah.empyriummod.difficulty.DifficultyAttachments
import quest.nekoniyah.empyriummod.difficulty.EmpyriumDifficulty
import quest.nekoniyah.empyriummod.economy.CurrencyCommand
import quest.nekoniyah.empyriummod.factions.utils.EmpyriumFactions
import quest.nekoniyah.empyriummod.factions.utils.FactionAttachments
import quest.nekoniyah.empyriummod.network.KeybindPayload
import quest.nekoniyah.empyriummod.network.NetworkUtils.registerPayload
import quest.nekoniyah.empyriummod.network.NetworkUtils.registerPayloadC2S
import quest.nekoniyah.empyriummod.network.OpenScreenPayload
import quest.nekoniyah.empyriummod.network.SelectFactionPayload

object EmpyriumMod : ModInitializer {
    const val MOD_ID: String = "empyrium-mod"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    val difficulty = EmpyriumDifficulty(DifficultyAttachments())
    val factions = EmpyriumFactions(FactionAttachments())

    var server: MinecraftServer? = null


    override fun onInitialize() {
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
            dispatcher.register(EmpyriumCommand(listOf(factions, difficulty, CurrencyCommand())).command)
        }


        LOGGER.info("Hello Fabric world!")
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

}
