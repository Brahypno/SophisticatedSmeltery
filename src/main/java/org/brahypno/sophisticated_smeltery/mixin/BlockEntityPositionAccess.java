package org.brahypno.sophisticated_smeltery.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Used only for the detached spawner owned by a portable upgrade. */
@Mixin(BlockEntity.class)
public interface BlockEntityPositionAccess {
    @Mutable @Accessor("worldPosition") void sophisticatedsmeltery$setPosition(BlockPos pos);
}
