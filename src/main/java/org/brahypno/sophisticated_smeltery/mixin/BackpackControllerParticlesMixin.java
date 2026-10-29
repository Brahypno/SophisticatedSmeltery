package org.brahypno.sophisticated_smeltery.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import org.brahypno.sophisticated_smeltery.client.BackpackControllerParticles;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BackpackBlock.class, remap = false)
public abstract class BackpackControllerParticlesMixin {
    @Shadow private static Vector3f getBackpackMiddleFacePoint(BlockPos pos, Direction facing, Vector3f vector) {
        throw new AssertionError();
    }

    @Inject(method = "renderUpgrades", at = @At("TAIL"))
    private static void sophisticatedsmeltery$particles(Level level, RandomSource random, BlockPos pos,
                                                        Direction facing, RenderInfo info, CallbackInfo ci) {
        if (!Minecraft.getInstance().isPaused()) {
            BackpackControllerParticles.render(level, random, info, point -> getBackpackMiddleFacePoint(pos, facing, point), false);
        }
    }
}
