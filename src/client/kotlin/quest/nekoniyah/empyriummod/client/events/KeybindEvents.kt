package quest.nekoniyah.empyriummod.client.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.client.EmpyriumModClient
import quest.nekoniyah.empyriummod.network.packets.KeybindPacket

object KeybindEvents {
    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { minecraft ->
            while (EmpyriumModClient.keybinding.ABILITY1_KEYBIND.consumeClick()) {
                if (minecraft.player == null) return@register;
                EmpyriumMod.NET_CHANNEL.clientHandle().send(KeybindPacket(1))
            }

            while (EmpyriumModClient.keybinding.RECIPE_CODEX_KEYBIND.consumeClick()) {
                if (minecraft.player == null) return@register
                minecraft.setScreen(EmpyriumModClient.getScreen("recipe_codex"))
            }

            while (EmpyriumModClient.keybinding.OPEN_SHOP_KEYBIND.consumeClick()) {
                if (minecraft.player == null) return@register
                minecraft.setScreen(EmpyriumModClient.getScreen("player_shop"))
            }
        }
    }
}