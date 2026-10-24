package org.brahypno.sophisticated_smeltery.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.joml.Vector3f;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;

/** The controller front is taken from its baked model, so resource packs apply. */
final class ControllerIconModel implements BakedModel {
    private final BakedModel base;
    private final List<BakedQuad> overlay;
    private final Map<Block, BakedModel> boundModels = new HashMap<>();
    private final ItemOverrides overrides = new ItemOverrides() {
        @Override public BakedModel resolve(BakedModel original, ItemStack stack, @Nullable ClientLevel level,
                                            @Nullable LivingEntity entity, int seed) {
            ItemStack controller = ControllerBinding.getController(stack);
            if (!(controller.getItem() instanceof BlockItem item)) return ControllerIconModel.this;
            return boundModels.computeIfAbsent(item.getBlock(), ControllerIconModel.this::withController);
        }
    };

    ControllerIconModel(BakedModel base) { this(base, List.of()); }
    private ControllerIconModel(BakedModel base, List<BakedQuad> overlay) { this.base = base; this.overlay = overlay; }

    private BakedModel withController(Block block) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        if (state.hasProperty(ControllerBlock.IN_STRUCTURE)) state = state.setValue(ControllerBlock.IN_STRUCTURE, true);
        BakedModel controller = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        // Tinkers' controller template identifies #front as its particle texture.
        // The first north quad is the brick body, not the overlaid controller face.
        TextureAtlasSprite sprite = controller.getParticleIcon();
        FaceBakery bakery = new FaceBakery();
        BlockElementFace face = new BlockElementFace(null, -1, "", new BlockFaceUV(new float[]{0, 0, 16, 16}, 0));
        List<BakedQuad> quads = new ArrayList<>();
        quads.add(bakery.bakeQuad(new Vector3f(4, 4, 8.6f), new Vector3f(12, 12, 8.6f), face, sprite,
                Direction.SOUTH, BlockModelRotation.X0_Y0, null, false, SophisticatedSmeltery.id("melting_upgrade")));
        quads.add(bakery.bakeQuad(new Vector3f(4, 4, 7.4f), new Vector3f(12, 12, 7.4f), face, sprite,
                Direction.NORTH, BlockModelRotation.X0_Y0, null, false, SophisticatedSmeltery.id("melting_upgrade")));
        BakedModel upgradeBase = Minecraft.getInstance().getItemRenderer().getModel(new ItemStack(ModItems.UPGRADE_BASE.get()), null, null, 0);
        return new ControllerIconModel(upgradeBase, quads);
    }

    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
        List<BakedQuad> original = base.getQuads(state, face, random);
        if (face != null || overlay.isEmpty()) return original;
        List<BakedQuad> result = new ArrayList<>(original);
        result.addAll(overlay);
        return result;
    }
    @Override public boolean useAmbientOcclusion() { return base.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return base.isGui3d(); }
    @Override public boolean usesBlockLight() { return base.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return base.getParticleIcon(); }
    @Override public ItemTransforms getTransforms() { return base.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return overlay.isEmpty() ? overrides : ItemOverrides.EMPTY; }
}
