package org.brahypno.sophisticated_smeltery.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.ModifierWorktableUpgradeContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Equipment is player-owned and excluded from automatic recipe input transfers. */
@Mixin(value = StorageContainerMenuBase.class, remap = false)
public abstract class TinkerStationEquipmentMixin {
    @Shadow protected abstract boolean mergeItemStack(Slot source, ItemStack stack, int start, int end, boolean reverse, boolean transferMax);

    @Inject(method = "mergeStackToOpenUpgradeTab", at = @At("HEAD"), cancellable = true)
    private void sophisticatedsmeltery$keepRecipeTransfersOutOfEquipment(Slot source, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        var menu = (StorageContainerMenuBase<?>)(Object)this;
        if (menu.getOpenContainer().orElse(null) instanceof TinkerStationUpgradeContainer station) {
            callback.setReturnValue(mergeItemStack(source, stack, station.getSlots().get(0).index, station.resultSlot().index + 1, false, true));
        } else if (menu.getOpenContainer().orElse(null) instanceof ModifierWorktableUpgradeContainer worktable) {
            callback.setReturnValue(mergeItemStack(source, stack, worktable.getSlots().get(0).index, worktable.resultSlot().index + 1, false, true));
        }
    }

    @Inject(method = "isInventorySlotInUpgradeTab", at = @At("HEAD"), cancellable = true)
    private void sophisticatedsmeltery$leaveEquipmentOnPlayer(Player player, Slot slot, CallbackInfoReturnable<Boolean> callback) {
        var menu = (StorageContainerMenuBase<?>)(Object)this;
        var owner = menu.getSlotUpgradeContainer(slot).orElse(null);
        if (slot.container == player.getInventory() && (owner instanceof TinkerStationUpgradeContainer || owner instanceof ModifierWorktableUpgradeContainer))
            callback.setReturnValue(false);
    }
}
