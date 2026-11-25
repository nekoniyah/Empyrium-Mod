package quest.nekoniyah.empyriummod.client.util.ui

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

object HudUtils {
    fun currency(texture: ResourceLocation, count: Int): FlowLayout =
        Containers.verticalFlow(Sizing.content(), Sizing.content()).apply {
            horizontalAlignment(HorizontalAlignment.CENTER)
            gap(-5)
            child(Components.texture(texture, 0, 0, 16, 16, 16, 16))
            child(Components.label(Component.literal(count.toString())))
        }

    fun stackedCurrencies(currencies: List<FlowLayout>): FlowLayout =
        Containers.horizontalFlow(Sizing.content(), Sizing.content()).apply {
            verticalAlignment(VerticalAlignment.CENTER)
            gap(8)
            currencies.forEach { child(it) }
        }
}