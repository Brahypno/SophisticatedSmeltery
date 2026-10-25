package org.brahypno.sophisticated_smeltery.mixin;

import org.brahypno.sophisticated_smeltery.furnace.PortableController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import slimeknights.tconstruct.smeltery.block.entity.controller.AlloyerBlockEntity;

/** Marks only detached alloyers so Mantle's change notifications save the upgrade instead of a world block. */
@Mixin(value = AlloyerBlockEntity.class, remap = false)
public abstract class AlloyerControllerMixin implements PortableController {
    @Unique private Runnable sophisticatedsmeltery$save;
    @Unique private int sophisticatedsmeltery$revision;
    public void sophisticatedsmeltery$attach(Runnable changed) { sophisticatedsmeltery$save = changed; }
    public boolean sophisticatedsmeltery$isPortable() { return sophisticatedsmeltery$save != null; }
    public void sophisticatedsmeltery$changed() {
        sophisticatedsmeltery$revision++;
        if (sophisticatedsmeltery$save != null) sophisticatedsmeltery$save.run();
    }
    public void sophisticatedsmeltery$setSizing(boolean sizing) { }
    public boolean sophisticatedsmeltery$isSizing() { return false; }
    public int sophisticatedsmeltery$getFluidRevision() { return sophisticatedsmeltery$revision; }
}
