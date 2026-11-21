package quest.nekoniyah.empyriummod.client.keymappings

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import quest.nekoniyah.empyriummod.client.EmpyriumModClient


class EmpyriumKeyMappings {
    var CATEGORY = EmpyriumModClient.id("keybinds.category")

    val ABILITY1_KEYBIND: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.${EmpyriumModClient.MOD_ID}.abilities.1.activate",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            CATEGORY.toLanguageKey()
        )
    )
}