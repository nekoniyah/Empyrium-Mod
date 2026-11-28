package quest.nekoniyah.empyriummod.economy.shop

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.commands.EmpyriumCommand
import quest.nekoniyah.empyriummod.economy.AdminShopServer
import quest.nekoniyah.empyriummod.network.packets.OpenScreenPacket

class AdminShopCommand : EmpyriumCommand(listOf(), "as") {
    override fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: return 0
        AdminShopServer.open(player, false)
        return 1
    }
}