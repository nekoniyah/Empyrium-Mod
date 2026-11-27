package quest.nekoniyah.empyriummod.factions.powers

import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.factions.utils.FactionAbility

class RecipeCodexPower : FactionAbility(
    displayName = "${EmpyriumMod.MOD_ID}.powers.recipe_codex.name",
    description = "${EmpyriumMod.MOD_ID}.powers.recipe_codex.description",
    identifier = "recipe_codex_power",

    ) {
    override fun execute(player: Player) {
    }
}