package quest.nekoniyah.empyriummod.client

import net.fabricmc.api.ClientModInitializer
import net.minecraft.resources.ResourceLocation
import quest.nekoniyah.empyriummod.client.events.KeybindEvents
import quest.nekoniyah.empyriummod.client.events.NetworkEvents
import quest.nekoniyah.empyriummod.client.keymappings.EmpyriumKeyMappings
import quest.nekoniyah.empyriummod.client.screens.EmpyriumScreen
import quest.nekoniyah.empyriummod.client.screens.FactionSelectionScreen

object EmpyriumModClient : ClientModInitializer {
    const val MOD_ID = "empyrium-mod"

    lateinit var keybinding: EmpyriumKeyMappings

    val screenRegistry: ArrayList<EmpyriumScreen> = arrayListOf(FactionSelectionScreen())

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.

        keybinding = EmpyriumKeyMappings()

        NetworkEvents.register()
        KeybindEvents.register()
    }


    fun getScreen(identifier: String): EmpyriumScreen? {
        return screenRegistry.find { it.identifier == identifier }
    }


    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}