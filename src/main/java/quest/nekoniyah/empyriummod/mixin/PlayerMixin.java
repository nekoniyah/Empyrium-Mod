package quest.nekoniyah.empyriummod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import quest.nekoniyah.empyriummod.EmpyriumMod;
import quest.nekoniyah.empyriummod.factions.powers.FactionAbility;
import quest.nekoniyah.empyriummod.storage.Ability;
import quest.nekoniyah.empyriummod.storage.FactionEntry;

import java.util.Objects;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Shadow
    public abstract void startFallFlying();

    @Shadow
    protected abstract void updatePlayerPose();

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void allowElytraFlight(CallbackInfoReturnable<Boolean> cir) {
        FactionEntry faction = getFaction();

        if (faction == null) return;

        boolean hasFallFlyingAbility = false;

        for (Ability ability : faction.getAbilities()) {
            FactionAbility workingAbility = EmpyriumMod.INSTANCE.getFactions().getWorkingAbility(ability);

            if (isWrongMixin(ability, "fallflying")) continue;

            assert workingAbility != null;

            hasFallFlyingAbility = true;
            break;
        }

        if (!hasFallFlyingAbility) return;

        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.onGround() || entity.onClimbable() || entity.isInWater()) {
            return;
        }

        this.startFallFlying();
        this.updatePlayerPose();
        cir.setReturnValue(true);
    }

    @Unique
    private FactionEntry getFaction() {
        Player player = ((Player) (Object) this);
        String playerFactionName = player.getAttached(EmpyriumMod.INSTANCE.getFactions().getAttachments().getFACTION());


        if (playerFactionName == null) return null;

        return EmpyriumMod.INSTANCE.getStorage().getFaction(playerFactionName);
    }

    @Unique
    private boolean isWrongMixin(Ability ability, String mixin) {
        FactionAbility workingAbility = EmpyriumMod.INSTANCE.getFactions().getWorkingAbility(ability);
        if (workingAbility == null) return true;

        if (workingAbility.getData().getMixin() == null) return true;
        return !Objects.equals(workingAbility.getData().getMixin(), mixin);
    }
}
