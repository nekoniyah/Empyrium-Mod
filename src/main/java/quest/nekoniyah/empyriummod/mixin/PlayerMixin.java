package quest.nekoniyah.empyriummod.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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


    @WrapMethod(method = "tryToStartFallFlying")
    private boolean allowElytraFlight(Operation<Boolean> original) {
        FactionEntry faction = getFaction((Player) (Object) this);

        if (faction == null) return original.call();

        boolean hasFallFlyingAbility = false;

        for (Ability ability : faction.getAbilities()) {
            FactionAbility workingAbility = EmpyriumMod.INSTANCE.getFactions().getWorkingAbility(ability);
            if (workingAbility == null) continue;
            if (isWrongMixin(workingAbility, "fallflying")) continue;
            hasFallFlyingAbility = true;
        }

        Player player = (Player) (Object) this;

        if (!hasFallFlyingAbility || player.onGround() || player.onClimbable() || player.isInWall() || player.isOnFire())
            return original.call();

        this.startFallFlying();
        this.updatePlayerPose();

        return hasFallFlyingAbility;
    }

    @Unique
    private FactionEntry getFaction(Player player) {
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

    @Unique
    private boolean isWrongMixin(FactionAbility ability, String mixin) {
        if (ability.getData().getMixin() == null) return true;
        return !Objects.equals(ability.getData().getMixin(), mixin);
    }
}
