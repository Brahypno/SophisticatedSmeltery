package org.brahypno.sophisticated_smeltery.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.brahypno.sophisticated_smeltery.spawner.SpawnerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Scope the existing advanced exemptions around the native world-spawner tick. */
@Mixin(SpawnerBlockEntity.class)
public abstract class WorldSpawnerTickMixin {
    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/BaseSpawner;serverTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V"))
    private static void sophisticatedsmeltery$adjacentUpgrade(BaseSpawner spawner, ServerLevel level, BlockPos pos) {
        SpawnerRuntime.tickWorld(level, pos, spawner);
    }
}
