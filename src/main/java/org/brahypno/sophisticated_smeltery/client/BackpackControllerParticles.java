package org.brahypno.sophisticated_smeltery.client;

import java.util.function.UnaryOperator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import org.joml.Vector3f;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.controller.FoundryControllerBlock;

/** Native controller fire and smoke, positioned at the cropped lower grille. */
public final class BackpackControllerParticles {
    private static final ResourceLocation TRANSMUTE_CONTROLLER = new ResourceLocation("esotericism_tinker", "transmute_controller");
    private BackpackControllerParticles() {}

    public static void render(Level level, RandomSource random, RenderInfo info,
                              UnaryOperator<Vector3f> toWorld, boolean worn) {
        var controller = BackpackControllerModel.controller(info);
        if (controller == null || !controller.hasProperty(ControllerBlock.ACTIVE)
                || !controller.getValue(ControllerBlock.ACTIVE)) return;
        // Native controllers emit in the lower six texture pixels. Scale those
        // coordinates by the same 12/16 factor as the backpack controller face.
        float y = (random.nextFloat() * 6 + 2) / 16f * 0.75f;
        Vector3f point = toWorld.apply(new Vector3f((random.nextFloat() * 0.6f - 0.3f) * 0.75f,
                worn ? y - 0.3f : y, worn ? -0.05f : -0.07f));
        boolean soulFire = controller.getBlock() instanceof FoundryControllerBlock
                || TRANSMUTE_CONTROLLER.equals(BuiltInRegistries.BLOCK.getKey(controller.getBlock()));
        var flame = soulFire ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        level.addParticle(ParticleTypes.SMOKE, point.x, point.y, point.z, 0, 0, 0);
        level.addParticle(flame, point.x, point.y, point.z, 0, 0, 0);
    }
}
