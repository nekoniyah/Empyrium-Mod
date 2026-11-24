package quest.nekoniyah.empyriummod.client.screens

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.chat.Component
import quest.nekoniyah.empyriummod.client.EmpyriumModClient
import quest.nekoniyah.empyriummod.network.SelectFactionPayload

class FactionSelectionScreen : EmpyriumScreen("Faction Selection") {
    val BACKGROUND = EmpyriumModClient.id("textures/gui/screen/faction/background.png")

    override val identifier = "faction_selection"

    fun selectFaction(name: String) {
        ClientPlayNetworking.send(SelectFactionPayload(name))
        onClose()
    }

    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    override fun build(rootComponent: FlowLayout) {
        rootComponent.surface(Surface.flat(0x77000000)).verticalAlignment(VerticalAlignment.CENTER)
            .horizontalAlignment(HorizontalAlignment.CENTER).sizing(Sizing.fill())

        rootComponent.child(
            Components.texture(BACKGROUND, 0, 0, this.width, this.height, this.width, this.height).sizing(Sizing.fill()).positioning(
                Positioning.relative(0, 0))
        )

        rootComponent.child(
            Containers.verticalFlow(Sizing.fill(), Sizing.fill()).children(
                listOf(
                    Components.button(
                    Component.literal("Angel"), { _ -> selectFaction("angel") })
                    .positioning(Positioning.relative(80, 16)),
                    Components.button(Component.literal("Human"), { _ -> selectFaction("human") })
                        .positioning(Positioning.relative(20, 55)),
                    Components.button(Component.literal("Faunid"), { _ -> selectFaction("faunid") })
                        .positioning(Positioning.relative(80, 85))
                )
            )
        )


    }
}