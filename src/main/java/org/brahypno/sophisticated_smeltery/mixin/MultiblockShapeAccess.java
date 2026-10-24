package org.brahypno.sophisticated_smeltery.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.MultiblockCuboid;

@Mixin(value = MultiblockCuboid.class, remap = false)
public interface MultiblockShapeAccess {
    @Accessor("hasFloor") boolean sophisticatedsmeltery$hasFloor();
    @Accessor("hasFrame") boolean sophisticatedsmeltery$hasFrame();
    @Accessor("hasCeiling") boolean sophisticatedsmeltery$hasCeiling();
    @Accessor("innerLimit") int sophisticatedsmeltery$innerLimit();
}
