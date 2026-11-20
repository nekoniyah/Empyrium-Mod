package quest.nekoniyah.empyriummod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import quest.nekoniyah.empyriummod.EmpyriumMod;
import quest.nekoniyah.empyriummod.difficulty.DifficultyAttachments;

import java.util.Objects;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void onDropAtDeath(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer) {
            String difficulty = ((ServerPlayer) (Object) this).getAttached(EmpyriumMod.INSTANCE.getDifficulty().getAttachments().getDIFFICULTY());
            if (Objects.equals(difficulty, "safe")) ci.cancel();
        }
    }
}
