package org.brahypno.sophisticated_smeltery.client;

import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

/** Delegate to the installed mod's real base model, including resource-pack replacements. */
public record UpgradeBaseGeometry(String host) implements IUnbakedGeometry<UpgradeBaseGeometry> {
    @Override public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
            Function<Material, TextureAtlasSprite> sprites, ModelState state, ItemOverrides overrides, ResourceLocation location) {
        return baker.bake(new ResourceLocation(host, "item/upgrade_base"), state);
    }
}
