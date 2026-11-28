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
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.Surface
import quest.nekoniyah.empyriummod.client.screens.PlayerShopScreen
import net.minecraft.network.chat.Component as Text

object Helpers {

    // ---------- UI helpers ----------

    fun column(width: Int): FlowLayout {
        val flow = Containers.verticalFlow(Sizing.fixed(width), Sizing.content())
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

    // ---------- Admin shop helpers ----------

    fun bordered(fill: Int, border: Int): Surface = Surface { context, component ->
        context.fill(
            component.x(), component.y(),
            component.x() + component.width(), component.y() + component.height(), fill
        )
        context.renderOutline(component.x(), component.y(), component.width(), component.height(), border)
    }

    fun pane(width: Int, height: Int): FlowLayout {
        val pane = Containers.verticalFlow(Sizing.fixed(width), Sizing.fixed(height))
        pane.surface(bordered(ShopTheme.PANE, ShopTheme.BORDER))
        pane.padding(Insets.of(4))
        pane.horizontalAlignment(HorizontalAlignment.CENTER)
        pane.gap(3)
        return pane
    }

    fun flatButton(
        text: String, width: Int, color: Int = ShopTheme.NEUTRAL,
        enabled: Boolean = true, onClick: (ButtonComponent) -> Unit
    ): ButtonComponent {
        val button = Components.button(Text.literal(text)) { onClick(it) }
        button.sizing(Sizing.fixed(width), Sizing.fixed(16))
        button.renderer(ButtonComponent.Renderer.flat(color, lighten(color), ShopTheme.SLOT))
        button.active = enabled
        return button
    }

    private fun lighten(color: Int): Int {
        fun channel(shift: Int) = minOf(255, ((color shr shift) and 0xFF) + 28) shl shift
        return (0xFF shl 24) or channel(16) or channel(8) or channel(0)
    }

    fun textBox(
        width: Int, initial: String, maxLength: Int = 32,
        digitsOnly: Boolean = false, hint: String = "", onChange: (String) -> Unit
    ): TextBoxComponent {
        val box = Components.textBox(Sizing.fixed(width))
        box.setMaxLength(maxLength)
        if (digitsOnly) box.setFilter { value -> value.all { it in '0'..'9' } }
        if (hint.isNotEmpty()) box.setHint(Text.literal(hint))
        box.text(initial)
        box.onChanged().subscribe { onChange(it) }
        return box
    }

    fun scroll(width: Int, height: Int, child: FlowLayout): ScrollContainer<FlowLayout> {
        val scroll = Containers.verticalScroll(Sizing.fixed(width), Sizing.fixed(height), child)
        scroll.scrollbarThiccness(3)
        scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(ShopTheme.ACCENT)))
        return scroll
    }

    /**
     * A clickable box. "selected" is checked every frame, so changing the selection
     * doesn't require rebuilding the grid (which would reset its scroll position).
     */
    fun slot(width: Int, height: Int, selected: () -> Boolean, onClick: () -> Unit): FlowLayout {
        val slot = Containers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(height))
        var hovered = false

        slot.surface(Surface { context, component ->
            val fill = if (hovered) ShopTheme.HOVER else ShopTheme.SLOT
            val border = if (selected()) ShopTheme.ACCENT else ShopTheme.BORDER
            bordered(fill, border).draw(context, component)
        })
        slot.verticalAlignment(VerticalAlignment.CENTER)
        slot.mouseEnter().subscribe { hovered = true }
        slot.mouseLeave().subscribe { hovered = false }
        slot.mouseDown().subscribe { _, _, mouseButton ->
            if (mouseButton == 0) onClick()
            mouseButton == 0
        }
        return slot
    }
}