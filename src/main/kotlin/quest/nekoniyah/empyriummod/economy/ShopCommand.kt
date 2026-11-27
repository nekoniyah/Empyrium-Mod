package quest.nekoniyah.empyriummod.economy

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.commands.EmpyriumCommand
import quest.nekoniyah.empyriummod.network.packets.OpenScreenPacket

class ShopCommand : EmpyriumCommand(listOf(), "shop") {
    override fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: return 0
        PlayerShopEvents.sendPlayerShopUpdate(player)
        EmpyriumMod.NET_CHANNEL.serverHandle(player).send(OpenScreenPacket("player_shop"))

        return 1
    }
}