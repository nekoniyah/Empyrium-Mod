package quest.nekoniyah.empyriummod.client.overlays

import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.Positioning
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.hud.Hud
import quest.nekoniyah.empyriummod.client.EmpyriumModClient.MOD_ID
import quest.nekoniyah.empyriummod.client.EmpyriumModClient.id
import quest.nekoniyah.empyriummod.client.util.ui.CurrencyHud
import quest.nekoniyah.empyriummod.client.util.ui.HudUtils

object EmpyriumOverlay {

    val location = id("huds.$MOD_ID.empyrium")

    var currencyDatas: MutableList<CurrencyHud> = mutableListOf()

    fun build(): FlowLayout {
        return Containers.verticalFlow(Sizing.fill(), Sizing.fill()).apply {
            horizontalAlignment(HorizontalAlignment.RIGHT)
            padding(Insets.of(10)) // distance from the screen edges
            positioning(Positioning.absolute(0, 0))
            child(HudUtils.stackedCurrencies(currencyDatas.map { data -> HudUtils.currency(data.icon, data.amount) }))
        }
    }

    fun addCurrencyData(data: CurrencyHud) {
        currencyDatas.add(data)
    }

    fun updateCurrencyData(data: CurrencyHud) {
        currencyDatas.remove(currencyDatas.find { it.identifier == data.identifier })
        addCurrencyData(data)
    }

    fun init() {
        Hud.add(location) { build() }
    }

    fun update() {
        Hud.remove(location)
        Hud.add(location) { build() }
    }
}
