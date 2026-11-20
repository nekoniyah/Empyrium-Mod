package quest.nekoniyah.empyriummod.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import quest.nekoniyah.empyriummod.difficulty.EmpyriumDifficulty

class EmpyriumCommand(empyriumDifficulty: EmpyriumDifficulty) {
    var command = LiteralArgumentBuilder.literal<CommandSourceStack>("empyrium").then(empyriumDifficulty.command)
}