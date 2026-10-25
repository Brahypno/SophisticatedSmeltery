package org.brahypno.sophisticated_smeltery.mixin;

import org.brahypno.sophisticated_smeltery.furnace.PortableController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.fluid.FluidTankBase;

/** Native single tanks must not send world-position packets for detached controllers. */
@Mixin(value = FluidTankBase.class, remap = false)
public abstract class PortableFluidTankMixin {
    @Shadow protected MantleBlockEntity parent;
    @Inject(method = "onContentsChanged", at = @At("HEAD"), cancellable = true)
    private void sophisticatedsmeltery$savePortableTank(CallbackInfo ci) {
        if (parent instanceof PortableController portable && portable.sophisticatedsmeltery$isPortable()) {
            portable.sophisticatedsmeltery$changed();
            ci.cancel();
        }
    }
}
