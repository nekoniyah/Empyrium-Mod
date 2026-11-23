package quest.nekoniyah.empyriummod.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Shadow
    public Input input;
    @Shadow
    @Final
    public ClientPacketListener connection;
    @Unique
    private boolean empyrium$wasJumping;

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void empyrium$glide(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        boolean jumping = input.jumping;
        if (jumping && !empyrium$wasJumping && !self.onGround() && !self.isFallFlying() && !self.isInWater() && !self.getAbilities().flying && !self.isPassenger() && !self.onClimbable()) {
            connection.send(new ServerboundPlayerCommandPacket(self, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
        empyrium$wasJumping = jumping;
    }
}