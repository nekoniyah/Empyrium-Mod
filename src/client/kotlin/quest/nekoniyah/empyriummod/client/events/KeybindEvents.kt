package quest.nekoniyah.empyriummod.client.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import quest.nekoniyah.empyriummod.client.EmpyriumModClient
import quest.nekoniyah.empyriummod.network.KeybindPayload

object KeybindEvents {
    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { minecraft ->
            while (EmpyriumModClient.keybinding.ABILITY1_KEYBIND.consumeClick()) {
                if (minecraft.player == null) return@register;
                ClientPlayNetworking.send(KeybindPayload(1))
            }
        }
    }
}