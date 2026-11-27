package quest.nekoniyah.empyriummod.client.util.ui

import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import quest.nekoniyah.empyriummod.client.screens.PlayerShopScreen
import net.minecraft.network.chat.Component as Text

object Helpers {

    // ---------- UI helpers ----------

    fun column(): FlowLayout {
        val flow = Containers.verticalFlow(Sizing.fixed(PlayerShopScreen.WIDTH), Sizing.content())
        flow.horizontalAlignment(HorizontalAlignment.CENTER)
        flow.gap(3)
        return flow
    }

    fun row(): FlowLayout {
        val flow = Containers.horizontalFlow(Sizing.content(), Sizing.content())
        flow.verticalAlignment(VerticalAlignment.CENTER)
        flow.gap(4)
        return flow
    }

    fun label(text: String, color: Int = PlayerShopScreen.WHITE): LabelComponent =
        Components.label(Text.literal(text)).color(Color.ofRgb(color))

    fun button(text: String, enabled: Boolean = true, onClick: () -> Unit): ButtonComponent =
        Components.button(Text.literal(text)) { onClick() }.also { it.active = enabled }

    fun numberBox(width: Int, initial: String, onChange: (String) -> Unit): Component {
        val box = Components.textBox(Sizing.fixed(width))
        box.setMaxLength(9) // keeps the value within Int range
        box.setFilter { value -> value.all { it in '0'..'9' } }
        box.text(initial)
        box.onChanged().subscribe { onChange(it) }
        return box
    }
}