package org.brahypno.sophisticated_smeltery.mixin;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The native item removal API has no slot parameter; this keeps multiple casting upgrades independent. */
@Mixin(value = UpgradeHandler.class, remap = false)
public abstract class CastingUpgradeExtractionMixin {
    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void sophisticatedsmeltery$keepCastingFluid(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        UpgradeHandler handler = (UpgradeHandler)(Object)this;
        if (handler.getStackInSlot(slot).getItem() instanceof CastingUpgradeItem
                && handler.getSlotWrappers().get(slot) instanceof CastingUpgradeWrapper casting && !casting.canRemove())
            cir.setReturnValue(ItemStack.EMPTY);
        if (handler.getStackInSlot(slot).getItem() instanceof AlloyerUpgradeItem
                && handler.getSlotWrappers().get(slot) instanceof AlloyerUpgradeWrapper alloyer && !alloyer.canRemove())
            cir.setReturnValue(ItemStack.EMPTY);
    }
}
