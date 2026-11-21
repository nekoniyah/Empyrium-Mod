package quest.nekoniyah.empyriummod.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import quest.nekoniyah.empyriummod.EmpyriumMod;
import quest.nekoniyah.empyriummod.factions.powers.AttributeCalc;
import quest.nekoniyah.empyriummod.factions.powers.AttributeCalcType;
import quest.nekoniyah.empyriummod.factions.powers.FactionAbility;
import quest.nekoniyah.empyriummod.storage.Ability;
import quest.nekoniyah.empyriummod.storage.FactionEntry;

import java.util.List;
import java.util.Objects;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public abstract boolean isFallFlying();

    @Shadow
    public abstract void addAdditionalSaveData(CompoundTag compoundTag);

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void onDropAtDeath(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo ci) {
        if ((Object) this instanceof Player) {
            String difficulty = ((Player) (Object) this).getAttached(EmpyriumMod.INSTANCE.getDifficulty().getAttachments().getDIFFICULTY());
            if (Objects.equals(difficulty, "safe")) ci.cancel();
        }
    }

    @WrapMethod(method = "hurt")
    private boolean multiplyPlayerDamage(DamageSource damageSource, float f, Operation<Boolean> original) {
        if ((Object) this instanceof Player player) {
            FactionEntry faction = getFaction(player);
            if (faction == null) return original.call(damageSource, f);
            float amount = modifyAmount(damageSource, ((Player) (Object) this), f, "hurt");
            original.call(damageSource, amount);
        }

        return original.call(damageSource, f);
    }


    @Unique
    private boolean isWrongMixin(Ability ability, String mixin) {
        FactionAbility workingAbility = EmpyriumMod.INSTANCE.getFactions().getWorkingAbility(ability);
        if (workingAbility == null) return true;

        if (workingAbility.getData().getMixin() == null) return true;
        return !Objects.equals(workingAbility.getData().getMixin(), mixin);
    }

    @Unique
    private FactionEntry getFaction(Player player) {
        String playerFactionName = player.getAttached(EmpyriumMod.INSTANCE.getFactions().getAttachments().getFACTION());

        if (playerFactionName == null) return null;

        return EmpyriumMod.INSTANCE.getStorage().getFaction(playerFactionName);

    }

    @Unique
    private float modifyAmount(DamageSource source, Player player, float amount, String mixin) {
        FactionEntry faction = getFaction(player);
        if (faction == null) return amount;
        float newAmount = amount;

        for (Ability ability : faction.getAbilities()) {
            FactionAbility workingAbility = EmpyriumMod.INSTANCE.getFactions().getWorkingAbility(ability);

            if (isWrongMixin(ability, mixin)) continue;

            assert workingAbility != null;
            AttributeCalc calc = (AttributeCalc) workingAbility.execute(player);

            List<String> tags = workingAbility.getData().getDamageTags();

            boolean dontMultiply = false;


            if (tags != null) {
                for (String damage : tags) {
                    ResourceKey<DamageType> dmg = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(damage));

                    if (source.is(dmg)) {
                        dontMultiply = true;
                    }
                }

            }


            if (!dontMultiply) {
                assert calc != null;
                if (calc.getType().equals(AttributeCalcType.MULTIPLY)) {
                    newAmount += amount * calc.getValue();
                }
            }
        }

        return newAmount;
    }

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void keepFallFlying(CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            FactionEntry faction = getFaction(player);

            if (faction == null) return;

            boolean hasFallFlyingAbility = false;

            for (Ability ability : faction.getAbilities()) {
                FactionAbility workingAbility = EmpyriumMod.INSTANCE.getFactions().getWorkingAbility(ability);

                if (workingAbility == null) continue;
                if (isWrongMixin(ability, "fallflying")) continue;

                hasFallFlyingAbility = true;
                break;
            }

            if (!hasFallFlyingAbility || player.onGround() || player.isInWater() || player.isInWall() || !this.isFallFlying())
                return;

            ci.cancel();
            player.setPose(Pose.FALL_FLYING);
            player.gameEvent(GameEvent.ELYTRA_GLIDE);
            player.startFallFlying();
        }
    }
}
