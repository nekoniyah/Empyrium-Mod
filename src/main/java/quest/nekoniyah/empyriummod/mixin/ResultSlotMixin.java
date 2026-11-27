package quest.nekoniyah.empyriummod.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import quest.nekoniyah.empyriummod.EmpyriumMod;
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase;
import quest.nekoniyah.empyriummod.factions.utils.EmpyriumFaction;
import quest.nekoniyah.empyriummod.factions.utils.FactionAbility;
import quest.nekoniyah.empyriummod.network.packets.RecipeCodexResponsePacket;

import java.util.Objects;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {

    @Shadow
    @Final
    private Player player;

    @Shadow
    private int removeCount;

    @Shadow
    @Final
    private CraftingContainer craftSlots;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void empyrium$onCraft(ItemStack itemStack, CallbackInfo ci) {
        if (removeCount <= 0 || !(player instanceof ServerPlayer serverPlayer)) return;

        String factionName = player.getAttached(EmpyriumMod.INSTANCE.getFactions().getAttachments().getFACTION());

        if(factionName == null) return;

        EmpyriumFaction faction = EmpyriumMod.INSTANCE.getFactions().getFaction(factionName);

        if (faction == null) return;

        boolean hasAbility = false;

        for (FactionAbility ability : faction.getAbilities()) {
            if (Objects.equals(ability.getMixin(), "onCraft")) {
                hasAbility = true;
            }
        }

        if (!hasAbility) return;

        boolean canHaveItForFree = EmpyriumDatabase.INSTANCE.checkRecipeCodexForFreeCraft(serverPlayer, itemStack);

        if (canHaveItForFree) {
            for (ItemStack ingredient : craftSlots.getItems()) {
                if (ingredient.isEmpty()) continue;

                if (ingredient.getItem().hasCraftingRemainingItem()) continue;
                player.getInventory().placeItemBackInInventory(ingredient.copyWithCount(1));
            }

            EmpyriumDatabase.INSTANCE.addOrUpdateItemToRecipeCodex(this.player, itemStack);

        } else {
            EmpyriumDatabase.INSTANCE.addOrUpdateItemToRecipeCodex(this.player, itemStack);
        }
    }
}