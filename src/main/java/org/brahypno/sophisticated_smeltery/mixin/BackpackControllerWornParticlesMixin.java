package org.brahypno.sophisticated_smeltery.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackLayerRenderer;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import org.brahypno.sophisticated_smeltery.client.BackpackControllerParticles;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BackpackLayerRenderer.class, remap = false)
public abstract class BackpackControllerWornParticlesMixin {
    @Shadow private static Vector3f getBackpackMiddleFacePoint(LivingEntity entity, Vector3f vector) {
        throw new AssertionError();
    }

    @Inject(method = "renderUpgrades", at = @At("TAIL"))
    private static void sophisticatedsmeltery$particles(LivingEntity entity, RenderInfo info, CallbackInfo ci) {
        if (!Minecraft.getInstance().isPaused() && entity.level().random.nextInt(32) == 0) {
            BackpackControllerParticles.render(entity.level(), entity.level().random, info,
                    point -> getBackpackMiddleFacePoint(entity, point), true);
        }
    }
}
