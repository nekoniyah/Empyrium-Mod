package quest.nekoniyah.empyriummod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import quest.nekoniyah.empyriummod.EmpyriumMod;
import quest.nekoniyah.empyriummod.factions.utils.EmpyriumFaction;
import quest.nekoniyah.empyriummod.factions.utils.FactionAbility;

import java.util.Objects;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Shadow
    public abstract void startFallFlying();


    @Shadow
    protected abstract void updatePlayerPose();

    @ModifyReturnValue(method = "tryToStartFallFlying", at = @At("RETURN"))
    public boolean empyrium$tryToStartFallFlying(boolean original) {
        EmpyriumFaction faction = getFaction((Player) (Object) this);
        if (faction == null) return original;

        boolean hasFallFlyingAbility = false;

        for (FactionAbility ability : faction.getAbilities()) {
            if (isWrongMixin(ability, "fallflying")) continue;
            hasFallFlyingAbility = true;
        }

        Player player = (Player) (Object) this;

        if (!hasFallFlyingAbility || player.onGround() || player.onClimbable() || player.isInWall() || player.isOnFire())
            return original;

        this.startFallFlying();
        this.updatePlayerPose();

        return hasFallFlyingAbility;
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
}
