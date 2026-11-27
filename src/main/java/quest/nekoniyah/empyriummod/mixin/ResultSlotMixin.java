package quest.nekoniyah.empyriummod.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import quest.nekoniyah.empyriummod.database.EmpyriumDatabase;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {

    @Shadow @Final private Player player;

    @Shadow private int removeCount;

    @Shadow @Final private CraftingContainer craftSlots;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void empyrium$onCraft(ItemStack itemStack, CallbackInfo ci) {
        if (removeCount <= 0 || !(player instanceof ServerPlayer serverPlayer)) return;

        boolean canHaveItForFree = EmpyriumDatabase.INSTANCE.checkRecipeCodexForFreeCraft(serverPlayer, itemStack);

        if (canHaveItForFree) {
            for (ItemStack ingredient : craftSlots.getItems()) {
                if (ingredient.isEmpty()) continue;

                // Milk buckets, honey bottles, etc. already leave an item behind
                if (ingredient.getItem().hasCraftingRemainingItem()) continue;

                // Adds to the inventory, and drops it if there's no room
                player.getInventory().placeItemBackInInventory(ingredient.copyWithCount(1));
            }
            EmpyriumDatabase.INSTANCE.addOrUpdateItemToRecipeCodex(this.player, itemStack, true);
            player.sendSystemMessage(Component.literal("You got an item for free"));
        } else {
            EmpyriumDatabase.INSTANCE.addOrUpdateItemToRecipeCodex(this.player, itemStack, false);
            player.sendSystemMessage(Component.literal("You got an item for not free"));
        }
    }
}