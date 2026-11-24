package quest.nekoniyah.empyriummod.client.screens

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.components.ImageWidget
import quest.nekoniyah.empyriummod.client.EmpyriumModClient
import quest.nekoniyah.empyriummod.client.util.ui.Button
import quest.nekoniyah.empyriummod.network.SelectFactionPayload

class FactionSelectionScreen : EmpyriumScreen("Faction Selection") {
    override val identifier = "faction_selection"

    val BACKGROUND = EmpyriumModClient.id("textures/gui/screen/faction/background.png")

    override fun init() {
        super.init()

        val background = ImageWidget.texture(this.width, this.height, BACKGROUND, this.width, this.height)

        val angelSelectButton = Button(
            this.width - 120 - 50, 30, "Angel", onPress = { _ -> selectFaction("angel") }).get()

        val humanSelectButton = Button(
            50, 120, "Human", onPress = { _ -> selectFaction("human") }).get()


        val faunidSelectButton = Button(
            this.width - 120 - 50, 210, "Faunid", onPress = { _ -> selectFaction("faunid") }).get()

        this.addRenderableOnly(background)
        this.addRenderableWidget(angelSelectButton)
        this.addRenderableWidget(humanSelectButton)
        this.addRenderableWidget(faunidSelectButton)
    }

    fun selectFaction(name: String) {
        ClientPlayNetworking.send(SelectFactionPayload(name))
        onClose()
    }
}