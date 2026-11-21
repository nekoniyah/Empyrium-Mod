package quest.nekoniyah.empyriummod.client.screens

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ImageWidget
import net.minecraft.network.chat.Component
import quest.nekoniyah.empyriummod.client.EmpyriumModClient
import quest.nekoniyah.empyriummod.network.SelectFactionPayload

class FactionSelectionScreen : EmpyriumScreen("Faction Selection") {
    companion object {
        const val NAME = "faction_selection"
    }

    val BACKGROUND = EmpyriumModClient.id("textures/gui/screen/faction/background.png")

    override fun init() {
        val background = ImageWidget.texture(
            this.width, this.height, BACKGROUND, this.width, this.height
        )

        background.setPosition(0, 0)

        val angelSelectButton = Button.builder(Component.literal("Angel")) { _ ->
            selectFaction("angel")
        }.bounds(this.width - 120 - 50, 30, 120, 20).build()

        val humanSelectButton = Button.builder(Component.literal("Human")) { _ ->
            selectFaction("human")
        }.bounds(50, 120, 120, 20).build()

        this.addRenderableOnly(background)
        this.addRenderableWidget(angelSelectButton)
        this.addRenderableWidget(humanSelectButton)
    }

    fun selectFaction(name: String) {
        ClientPlayNetworking.send(SelectFactionPayload(name))
        onClose()
    }
}