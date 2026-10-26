package org.brahypno.sophisticated_smeltery.mixin;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import org.brahypno.sophisticated_smeltery.furnace.FluidStorageUpgrade;
import org.brahypno.sophisticated_smeltery.furnace.FurnaceFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StorageWrapper.class, remap = false)
public abstract class StorageFluidWrapperMixin {
    @Unique private List<FluidStorageUpgrade> sophisticatedsmeltery$upgrades = List.of();
    @Unique private IStorageFluidHandler sophisticatedsmeltery$fluid;

    @Inject(method = "getFluidHandler", at = @At("RETURN"), cancellable = true)
    private void sophisticatedsmeltery$fluids(CallbackInfoReturnable<Optional<IStorageFluidHandler>> cir) {
        // Linked storage already delegates to the canonical host's handler.
        if (cir.getReturnValue().isPresent()) return;
        var upgrades = ((IStorageWrapper)this).getUpgradeHandler().getSlotWrappers().values().stream()
                .filter(FluidStorageUpgrade.class::isInstance).map(FluidStorageUpgrade.class::cast).toList();
        if (!upgrades.equals(sophisticatedsmeltery$upgrades)) {
            sophisticatedsmeltery$upgrades = upgrades;
            sophisticatedsmeltery$fluid = null;
            for (var upgrade : upgrades) sophisticatedsmeltery$fluid =
                    new FurnaceFluidHandler(upgrade, sophisticatedsmeltery$fluid, ItemStack.EMPTY);
        }
        cir.setReturnValue(Optional.ofNullable(sophisticatedsmeltery$fluid));
    }
}
