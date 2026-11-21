package quest.nekoniyah.empyriummod.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import quest.nekoniyah.empyriummod.EmpyriumMod;
import quest.nekoniyah.empyriummod.factions.powers.AttributeCalc;
import quest.nekoniyah.empyriummod.factions.powers.AttributeCalcType;
import quest.nekoniyah.empyriummod.factions.powers.FactionAbility;
import quest.nekoniyah.empyriummod.storage.Ability;
import quest.nekoniyah.empyriummod.storage.FactionEntry;

import java.util.Objects;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public abstract boolean isFallFlying();

    @Unique
    protected boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose) {
        Player player = ((Player) (Object) this);
        return player.level().noCollision(player, player.getDimensions(pose).makeBoundingBox(player.position()).deflate(1.0E-7));
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void onDropAtDeath(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo ci) {
        if ((Object) this instanceof Player) {
            String difficulty = ((Player) (Object) this).getAttached(EmpyriumMod.INSTANCE.getDifficulty().getAttachments().getDIFFICULTY());
            if (Objects.equals(difficulty, "safe")) ci.cancel();
        }
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float multiplyPlayerDamage(float amount) {
        if ((Object) this instanceof Player) {
            FactionEntry faction = getFaction();
            if (faction == null) return amount;
            return modifyAmount(((Player) (Object) this), amount, "hurt");
        }

        return amount;
    }


    @Unique
    private boolean isWrongMixin(Ability ability, String mixin) {
        FactionAbility workingAbility = EmpyriumMod.INSTANCE.getFactions().getWorkingAbility(ability);
        if (workingAbility == null) return true;

        if (workingAbility.getData().getMixin() == null) return true;
        return !Objects.equals(workingAbility.getData().getMixin(), mixin);
    }

    @Unique
    private FactionEntry getFaction() {
        Player player = ((Player) (Object) this);
        String playerFactionName = player.getAttached(EmpyriumMod.INSTANCE.getFactions().getAttachments().getFACTION());

        if (playerFactionName == null) return null;

        return EmpyriumMod.INSTANCE.getStorage().getFaction(playerFactionName);

    }

    @Unique
    private float modifyAmount(Player player, float amount, String mixin) {
        FactionEntry faction = getFaction();
        if (faction == null) return amount;
        float newAmount = amount;

        for (Ability ability : faction.getAbilities()) {
            FactionAbility workingAbility = EmpyriumMod.INSTANCE.getFactions().getWorkingAbility(ability);

            if (isWrongMixin(ability, mixin)) continue;

            assert workingAbility != null;
            AttributeCalc calc = (AttributeCalc) workingAbility.execute(player);

            if (calc.getType().equals(AttributeCalcType.MULTIPLY)) {
                newAmount += amount * calc.getValue();
            }
        }

        return newAmount;
    }

    @WrapMethod(method = "updateFallFlying")
    private void keepFallFlying(Operation<Void> original) {
        if ((Object) this instanceof Player player) {
            FactionEntry faction = getFaction();

            if (faction == null) {
                original.call();
                return;
            }

            boolean hasFallFlyingAbility = false;

            for (Ability ability : faction.getAbilities()) {
                FactionAbility workingAbility = EmpyriumMod.INSTANCE.getFactions().getWorkingAbility(ability);

                if (isWrongMixin(ability, "fallflying")) continue;

                assert workingAbility != null;

                hasFallFlyingAbility = true;
                break;
            }

            if (!hasFallFlyingAbility || player.onGround() || player.isInWater() || player.isInWall()) {
                original.call();
                return;
            }

            if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {
                Pose pose;
                if (this.isFallFlying()) {
                    pose = Pose.FALL_FLYING;
                } else if (player.isSleeping()) {
                    pose = Pose.SLEEPING;
                } else if (player.isSwimming()) {
                    pose = Pose.SWIMMING;
                } else if (player.isAutoSpinAttack()) {
                    pose = Pose.SPIN_ATTACK;
                } else if (player.isShiftKeyDown() && !player.getAbilities().flying) {
                    pose = Pose.CROUCHING;
                } else {
                    pose = Pose.STANDING;
                }

                Pose pose2;
                if (!player.isSpectator() && !player.isPassenger() && !this.canPlayerFitWithinBlocksAndEntitiesWhen(pose)) {
                    if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)) {
                        pose2 = Pose.CROUCHING;
                    } else {
                        pose2 = Pose.SWIMMING;
                    }
                } else {
                    pose2 = pose;
                }

                player.setPose(pose2);
            }


            if (!this.isFallFlying()) original.call();
        }
    }
}
