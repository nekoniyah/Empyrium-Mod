package quest.nekoniyah.empyriummod.client.util.ui

import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

class Button(
    val screen: EmpyriumScreenLayout, val text: String, val onPress: Button.OnPress, layer: Int
) : ScreenLayoutElement(7, 3, screen, true, layer) {

    override fun build(row: Int, col: Int): Button {
        return Button.Builder(Component.literal(text), onPress)
            .bounds(row * screen.boxWidth, col * screen.boxHeight, width, height).build()
    }
}