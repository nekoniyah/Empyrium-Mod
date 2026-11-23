package quest.nekoniyah.empyriummod.factions.powers

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.factions.utils.FactionAbility


class LaunchPower : FactionAbility(
    displayName = "${EmpyriumMod.MOD_ID}.powers.elytra_flight.name",
    description = "${EmpyriumMod.MOD_ID}.powers.elytra_flight.description",
    identifier = "launch",
    keybind = 1,
    passive = false,
    cooldown = 0
) {
    override fun execute(player: Player) {
        if (player is ServerPlayer) {

            if (player.onGround()) return

            val boost = 1.3

            val dir = player.lookAngle

            player.deltaMovement = player.deltaMovement.add(dir.x * boost, dir.y * boost, dir.z * boost);
            player.hurtMarked = true; // flags the velocity change for syncing
            player.connection.send(ClientboundSetEntityMotionPacket(player));
        }
    }
}