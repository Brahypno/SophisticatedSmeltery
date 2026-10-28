package org.brahypno.sophisticated_smeltery.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
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
import net.minecraft.world.level.block.state.BlockState;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.joml.Vector3f;

/** Project native workstation faces onto the upgrade plate. */
final class CastingIconModel implements BakedModel {
    private final BakedModel base;
    private final List<BakedQuad> overlay = new ArrayList<>();

    CastingIconModel(BakedModel base, BakedModel table, BlockState state, @Nullable TextureAtlasSprite advancedFrame) {
        this(base, table, state, advancedFrame, false);
    }

    CastingIconModel(BakedModel base, BakedModel table, BlockState state, @Nullable TextureAtlasSprite advancedFrame, boolean sideView) {
        this(base, table, state, advancedFrame, sideView, false);
    }

    CastingIconModel(BakedModel base, BakedModel table, BlockState state, @Nullable TextureAtlasSprite advancedFrame,
                     boolean sideView, boolean showLegs) {
        this.base = base;
        if (advancedFrame != null) addAdvancedFrame(overlay, advancedFrame);
        for (Direction face : showLegs ? List.of(Direction.UP, Direction.SOUTH) : List.of(sideView ? Direction.SOUTH : Direction.UP)) {
            List<BakedQuad> faces = new ArrayList<>(table.getQuads(state, face, RandomSource.create(0)));
            table.getQuads(state, null, RandomSource.create(0)).stream().filter(quad -> quad.getDirection() == face).forEach(faces::add);
            for (BakedQuad quad : faces) {
                overlay.add(project(quad, false, sideView, showLegs));
                overlay.add(project(quad, true, sideView, showLegs));
            }
        }
    }

    private static BakedQuad project(BakedQuad quad, boolean back, boolean sideView, boolean showLegs) {
        int[] source = quad.getVertices();
        int[] result = source.clone();
        int stride = source.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int from = (back ? 3 - vertex : vertex) * stride;
            int to = vertex * stride;
            System.arraycopy(source, from, result, to, stride);
            float x = Float.intBitsToFloat(source[from]);
            float y = Float.intBitsToFloat(source[from + 1]);
            float z = Float.intBitsToFloat(source[from + 2]);
            result[to] = Float.floatToRawIntBits(0.25f + x * 0.5f);
            // The native station retextures only its legs, so retain both its top and front.
            result[to + 1] = Float.floatToRawIntBits(0.25f + (sideView ? y : showLegs ? y * 0.75f + (1 - z) * 0.25f : 1 - z) * 0.5f);
            float depth = sideView || showLegs ? z : y;
            result[to + 2] = Float.floatToRawIntBits(back ? 0.45f - depth * 0.005f : 0.55f + depth * 0.005f);
            result[to + stride - 1] = back ? 0x00810000 : 0x007f0000;
        }
        return new BakedQuad(result, quad.getTintIndex(), back ? Direction.NORTH : Direction.SOUTH, quad.getSprite(), false, false);
    }

    /** Keep the real Sophisticated advanced frame; exclude the magnet artwork in its center. */
    static void addAdvancedFrame(List<BakedQuad> overlay, TextureAtlasSprite sprite) {
        frame(overlay, sprite, 0, 0, 16, 3);
        frame(overlay, sprite, 0, 13, 16, 16);
        frame(overlay, sprite, 0, 3, 3, 13);
        frame(overlay, sprite, 13, 3, 16, 13);
    }

    private static void frame(List<BakedQuad> overlay, TextureAtlasSprite sprite, int left, int top, int right, int bottom) {
        FaceBakery bakery = new FaceBakery();
        BlockElementFace face = new BlockElementFace(null, -1, "", new BlockFaceUV(new float[]{left, top, right, bottom}, 0));
        for (Direction direction : List.of(Direction.SOUTH, Direction.NORTH)) {
            float depth = direction == Direction.SOUTH ? 8.6f : 7.4f;
            overlay.add(bakery.bakeQuad(new Vector3f(left, 16 - bottom, depth), new Vector3f(right, 16 - top, depth),
                    face, sprite, direction, BlockModelRotation.X0_Y0, null, false, SophisticatedSmeltery.id("advanced_seared_casting_upgrade")));
        }
    }

    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
        List<BakedQuad> original = base.getQuads(state, face, random);
        if (face != null) return original;
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
    @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
}
