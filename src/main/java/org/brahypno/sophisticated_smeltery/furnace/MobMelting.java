package org.brahypno.sophisticated_smeltery.furnace;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public interface MobMelting {
    void tick(@Nullable Entity host, ServerLevel level, BlockPos pos);
    void save(CompoundTag state);
}
