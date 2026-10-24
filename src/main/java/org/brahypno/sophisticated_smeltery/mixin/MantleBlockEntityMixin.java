package org.brahypno.sophisticated_smeltery.mixin;

import org.brahypno.sophisticated_smeltery.furnace.PortableController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.mantle.block.entity.MantleBlockEntity;

@Mixin(value = MantleBlockEntity.class, remap = false)
public abstract class MantleBlockEntityMixin {
    @Inject(method = "setChangedFast", at = @At("HEAD"), cancellable = true)
    private void sophisticatedsmeltery$saveUpgrade(CallbackInfo ci) {
        if ((Object)this instanceof PortableController portable && portable.sophisticatedsmeltery$isPortable()) {
            portable.sophisticatedsmeltery$changed();
            ci.cancel();
        }
    }
}
