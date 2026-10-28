package org.brahypno.sophisticated_smeltery.client;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.BakedModelWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationAppearance;

/** Delegates native material/texture overrides before projecting the workstation onto the host plate. */
final class TinkerStationIconModel extends BakedModelWrapper<BakedModel> {
    private final Map<CompoundTag, BakedModel> appearances = new HashMap<>();
    private final ItemOverrides overrides;

    TinkerStationIconModel(BakedModel fallback, BakedModel plate, @Nullable TextureAtlasSprite frame, boolean anvil) {
        super(fallback);
        overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel original, ItemStack stack, @Nullable ClientLevel level,
                                                @Nullable LivingEntity entity, int seed) {
                ItemStack source = TinkerStationAppearance.source(stack);
                return appearances.computeIfAbsent(source.save(new CompoundTag()), key -> {
                    var state = ((BlockItem)source.getItem()).getBlock().defaultBlockState();
                    if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                        state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
                    BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
                    BakedModel resolved = model.getOverrides().resolve(model, source, level, entity, seed);
                    return new CastingIconModel(plate, resolved == null ? model : resolved, state, frame, anvil, !anvil);
                });
            }
        };
    }
    @Override public ItemOverrides getOverrides() { return overrides; }
}
