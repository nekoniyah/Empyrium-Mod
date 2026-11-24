package quest.nekoniyah.empyriummod.client.util.ui

import net.minecraft.client.gui.components.AbstractWidget


abstract class ScreenLayoutElement(
    val xSize: Int, val ySize: Int, screen: EmpyriumScreenLayout, val widget: Boolean, val layer: Int = 1
) {
    var width: Int = xSize * screen.boxWidth
    var height: Int = ySize * screen.boxHeight

    abstract fun build(row: Int, col: Int): AbstractWidget
}