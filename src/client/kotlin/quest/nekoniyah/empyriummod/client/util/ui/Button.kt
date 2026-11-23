package quest.nekoniyah.empyriummod.client.util.ui

import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

class Button(val x: Int, val y: Int, val text: String, val onPress: Button.OnPress) {
    fun get(width: Int = 120, height: Int = 20): Button {
        return Button.Builder(Component.literal(text), onPress).bounds(this.x, this.y, width, height).build()
    }
}