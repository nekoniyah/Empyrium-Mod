package quest.nekoniyah.empyriummod.client.screens

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.container.FlowLayout
import net.minecraft.network.chat.Component

abstract class EmpyriumScreen(title: String) : BaseOwoScreen<FlowLayout>(Component.literal(title)) {
    abstract val identifier: String
}