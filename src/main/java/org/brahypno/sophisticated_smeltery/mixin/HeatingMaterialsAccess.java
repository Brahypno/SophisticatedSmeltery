package org.brahypno.sophisticated_smeltery.mixin;

import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock;

@Mixin(value = HeatingStructureMultiblock.class, remap = false)
public interface HeatingMaterialsAccess {
    @Invoker("isValidBlock") boolean sophisticatedsmeltery$isMaterial(Block block);
    @Invoker("isValidFloor") boolean sophisticatedsmeltery$isFloor(Block block);
    @Invoker("isValidWall") boolean sophisticatedsmeltery$isWall(Block block);
    @Invoker("isValidTank") boolean sophisticatedsmeltery$isTank(Block block);
}
