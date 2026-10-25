package org.brahypno.sophisticated_smeltery.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import org.brahypno.sophisticated_smeltery.spawner.SpawnerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseSpawner.class)
public abstract class SpawnerActivationMixin {
    @Inject(method = "isNearPlayer", at = @At("HEAD"), cancellable = true)
    private void sophisticatedsmeltery$activateAdvanced(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (SpawnerRuntime.ignoresPlayer((BaseSpawner)(Object)this)) cir.setReturnValue(true);
    }
}
