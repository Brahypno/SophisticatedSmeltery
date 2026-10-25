package org.brahypno.sophisticated_smeltery.mixin;

import java.util.Optional;
import net.minecraft.world.level.SpawnData;
import org.brahypno.sophisticated_smeltery.spawner.SpawnerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnData.class)
public abstract class SpawnerRulesMixin {
    @Inject(method = "getCustomSpawnRules", at = @At("RETURN"), cancellable = true)
    private void sophisticatedsmeltery$advancedRules(CallbackInfoReturnable<Optional<SpawnData.CustomSpawnRules>> cir) {
        cir.setReturnValue(SpawnerRuntime.rules(cir.getReturnValue()));
    }
}
