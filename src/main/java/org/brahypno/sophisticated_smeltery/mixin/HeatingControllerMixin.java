package org.brahypno.sophisticated_smeltery.mixin;

import net.minecraftforge.fluids.FluidStack;
import org.brahypno.sophisticated_smeltery.furnace.PortableController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

@Mixin(value = HeatingStructureBlockEntity.class, remap = false)
public abstract class HeatingControllerMixin implements PortableController {
    @Unique private Runnable sophisticatedsmeltery$save;
    @Unique private boolean sophisticatedsmeltery$sizing;
    @Unique private int sophisticatedsmeltery$fluidRevision;
    public void sophisticatedsmeltery$attach(Runnable changed) { sophisticatedsmeltery$save = changed; }
    public boolean sophisticatedsmeltery$isPortable() { return sophisticatedsmeltery$save != null; }
    public void sophisticatedsmeltery$changed() {
        if (sophisticatedsmeltery$save != null && !sophisticatedsmeltery$sizing) sophisticatedsmeltery$save.run();
    }
    public void sophisticatedsmeltery$setSizing(boolean sizing) { sophisticatedsmeltery$sizing = sizing; }
    public boolean sophisticatedsmeltery$isSizing() { return sophisticatedsmeltery$sizing; }
    public int sophisticatedsmeltery$getFluidRevision() { return sophisticatedsmeltery$fluidRevision; }

    @Inject(method = "notifyFluidsChanged", at = @At("HEAD"), cancellable = true)
    private void sophisticatedsmeltery$portableFluids(ISmelteryTankHandler.FluidChange type, FluidStack fluid, CallbackInfo ci) {
        if (sophisticatedsmeltery$isPortable()) {
            sophisticatedsmeltery$fluidRevision++;
            sophisticatedsmeltery$changed();
            ci.cancel();
        }
    }
}
