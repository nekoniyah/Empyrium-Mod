package quest.nekoniyah.empyriummod.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.registries.Registries;
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
import quest.nekoniyah.empyriummod.factions.utils.EmpyriumFaction;
import quest.nekoniyah.empyriummod.factions.utils.AttributeCalc;
import quest.nekoniyah.empyriummod.factions.utils.AttributeCalcType;
import quest.nekoniyah.empyriummod.factions.utils.FactionAbility;

import java.util.List;
import java.util.Objects;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public abstract boolean isFallFlying();

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
            EmpyriumFaction faction = getFaction(player);
            float amount = modifyAmount(damageSource, ((Player) (Object) this), f, "hurt");
            original.call(damageSource, amount);
        }

        return original.call(damageSource, f);
    }

    @Unique
    private EmpyriumFaction getFaction(Player player) {
        String factionIdentifier = player.getAttached(EmpyriumMod.INSTANCE.getFactions().getAttachments().getFACTION());
        return factionIdentifier != null ? EmpyriumMod.INSTANCE.getFactions().getFaction(factionIdentifier) : null;
    }

    @Unique
    private boolean isWrongMixin(FactionAbility ability, String mixin) {
        if (ability.getMixin() == null) return true;
        return !Objects.equals(ability.getMixin(), mixin);
    }

    @Unique
    private float modifyAmount(DamageSource source, Player player, float amount, String mixin) {
        EmpyriumFaction faction = getFaction(player);
        if (faction == null) return amount;
        float newAmount = amount;

        for (FactionAbility ability : faction.getAbilities()) {
            if (isWrongMixin(ability, mixin)) continue;
            AttributeCalc calc = (AttributeCalc) ability.execute(player);

            List<String> tags = ability.getDamageTags();

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
            EmpyriumFaction faction = getFaction(player);

            if (faction == null) return;

            boolean hasFallFlyingAbility = false;

            for (FactionAbility ability : faction.getAbilities()) {
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
