package quest.nekoniyah.empyriummod.client.screens

import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

abstract class EmpyriumScreen(title: String) : Screen(Component.literal(title)) {
    companion object {
        const val NAME = ""
    }
}