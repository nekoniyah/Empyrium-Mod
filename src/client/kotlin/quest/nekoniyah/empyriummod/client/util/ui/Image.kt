package quest.nekoniyah.empyriummod.client.util.ui

import net.minecraft.client.gui.components.ImageWidget
import net.minecraft.resources.ResourceLocation


class Image(
    xSize: Int, ySize: Int, val screen: EmpyriumScreenLayout, val texture: ResourceLocation, layer: Int
) : ScreenLayoutElement(xSize, ySize, screen, false, layer) {
    override fun build(row: Int, col: Int): ImageWidget {
        val targetWidth = xSize * screen.boxWidth
        val targetHeight = ySize * screen.boxHeight

        val widget = ImageWidget.texture(targetWidth, targetHeight, texture, targetWidth, targetHeight)
        widget.setPosition(row * screen.boxWidth, col * screen.boxHeight)
        return widget
    }


}