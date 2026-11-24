package quest.nekoniyah.empyriummod.client.screens

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import quest.nekoniyah.empyriummod.client.EmpyriumModClient
import quest.nekoniyah.empyriummod.client.util.ui.Button
import quest.nekoniyah.empyriummod.client.util.ui.EmpyriumScreenLayout
import quest.nekoniyah.empyriummod.client.util.ui.Image
import quest.nekoniyah.empyriummod.network.SelectFactionPayload

class FactionSelectionScreen : EmpyriumScreenLayout(16 * 4, 9 * 4, "Faction Selection", "faction_selection") {
    val BACKGROUND = EmpyriumModClient.id("textures/gui/screen/faction/background.png")

    init {
        val background = Image(this.rows, this.columns, this, BACKGROUND, 0)

        // this.width - 120 - 50,

        val buttonLayer = 2

        val angelSelectButton = Button(
            this, "Angel", onPress = { _ -> selectFaction("angel") }, buttonLayer
        )

        // 50, 120,
        val humanSelectButton = Button(this, "Human", onPress = { _ -> selectFaction("human") }, buttonLayer)

        // this.width - 120 - 50, 210,
        val faunidSelectButton = Button(this, "Faunid", onPress = { _ -> selectFaction("faunid") }, buttonLayer)


        setElement(2, 0, background)
        setElement(49, 4, angelSelectButton)
        setElement(11, 17, humanSelectButton)
        setElement(49, 28, faunidSelectButton)
    }

    fun selectFaction(name: String) {
        ClientPlayNetworking.send(SelectFactionPayload(name))
        onClose()
    }
}