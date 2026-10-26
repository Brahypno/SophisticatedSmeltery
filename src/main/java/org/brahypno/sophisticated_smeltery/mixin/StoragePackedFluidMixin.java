package org.brahypno.sophisticated_smeltery.mixin;

import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import org.brahypno.sophisticated_smeltery.compat.StorageFluidInvalidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WoodStorageBlockEntity.class, remap = false)
public abstract class StoragePackedFluidMixin {
    @Inject(method = "setPacked", at = @At("RETURN"))
    private void sophisticatedsmeltery$packing(boolean packed, CallbackInfo ci) {
        ((StorageFluidInvalidator)this).sophisticatedsmeltery$invalidateFluids();
    }
}
