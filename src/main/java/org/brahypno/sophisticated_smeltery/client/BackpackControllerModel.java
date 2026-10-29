package org.brahypno.sophisticated_smeltery.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.data.ModelProperty;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeItem;
import org.joml.Vector3f;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;

/** Shared geometry for the placed, held, inventory and worn backpack models. */
public final class BackpackControllerModel {
    public static final ModelProperty<BlockState> CONTROLLER = new ModelProperty<>();
    private static final Map<BlockState, Map<Direction, List<BakedQuad>>> CACHE = new ConcurrentHashMap<>();
    private BackpackControllerModel() {}

    @Nullable public static BlockState controller(RenderInfo info) {
        for (ItemStack upgrade : info.getUpgradeItems()) {
            if (!(upgrade.getItem() instanceof MeltingUpgradeItem)) continue;
            ItemStack controller = ControllerBinding.getController(upgrade);
            if (controller.getItem() instanceof BlockItem item) {
                BlockState state = item.getBlock().defaultBlockState();
                if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                    state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
                if (state.hasProperty(ControllerBlock.IN_STRUCTURE)) state = state.setValue(ControllerBlock.IN_STRUCTURE, true);
                if (state.hasProperty(ControllerBlock.ACTIVE)) state = state.setValue(ControllerBlock.ACTIVE, ControllerBinding.isActive(upgrade));
                return state;
            }
        }
        return null;
    }

    public static void clearCache() { CACHE.clear(); }
    public static List<BakedQuad> quads(BlockState controller, Direction facing) {
        return CACHE.computeIfAbsent(controller, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(facing, key -> bake(controller, facing));
    }

    private static List<BakedQuad> bake(BlockState controller, Direction facing) {
        var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(controller);
        var bakery = new FaceBakery();
        var rotation = switch (facing) {
            case EAST -> BlockModelRotation.X0_Y90;
            case SOUTH -> BlockModelRotation.X0_Y180;
            case WEST -> BlockModelRotation.X0_Y270;
            default -> BlockModelRotation.X0_Y0;
        };
        List<BakedQuad> result = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            var originals = model.getQuads(controller, direction, RandomSource.create(0));
            var sprite = direction == Direction.NORTH || originals.isEmpty() ? model.getParticleIcon() : originals.get(0).getSprite();
            // A twelve-pixel controller with its upper half removed. Enclose the
            // entire front pouch (including its clips), almost flush with it.
            float[] uv = direction.getAxis().isHorizontal() ? new float[]{0, 8, 16, 16} : new float[]{0, 0, 16, 16};
            var face = new BlockElementFace(null, -1, "", new BlockFaceUV(uv, 0));
            result.add(bakery.bakeQuad(new Vector3f(2, 0, 2.65f), new Vector3f(14, 6.01f, 5.01f),
                    face, sprite, direction, rotation, null, true, SophisticatedSmeltery.id("backpack_controller")));
        }
        return List.copyOf(result);
    }
}
