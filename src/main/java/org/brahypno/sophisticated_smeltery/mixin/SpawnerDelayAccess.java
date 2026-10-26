package org.brahypno.sophisticated_smeltery.mixin;

import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BaseSpawner.class)
public interface SpawnerDelayAccess {
    @Accessor("spawnDelay") int sophisticatedsmeltery$getSpawnDelay();
}
