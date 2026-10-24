package org.brahypno.sophisticated_smeltery.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.Mutable;
import slimeknights.tconstruct.smeltery.block.entity.module.MultitankFuelModule;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock;

@Mixin(value = HeatingStructureBlockEntity.class, remap = false)
public interface HeatingControllerAccess {
    @Accessor("multiblock") HeatingStructureMultiblock<?> sophisticatedsmeltery$getMultiblock();
    @Accessor("fuelRate") int sophisticatedsmeltery$getFuelRate();
    @Invoker("setStructure") void sophisticatedsmeltery$applyDimensions(HeatingStructureMultiblock.StructureData dimensions);
    @Mutable @Accessor("fuelModule") void sophisticatedsmeltery$setFuel(MultitankFuelModule fuel);
    @Accessor("tick") void sophisticatedsmeltery$setTick(int tick);
    @Invoker("heat") void sophisticatedsmeltery$heat();
}
