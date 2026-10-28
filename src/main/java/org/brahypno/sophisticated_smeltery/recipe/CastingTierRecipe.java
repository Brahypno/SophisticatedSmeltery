package org.brahypno.sophisticated_smeltery.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;

/** Visible tier recipe with state transfer, without Core's extra JEI display recipe. */
public final class CastingTierRecipe extends ShapedRecipe {
    public CastingTierRecipe(ShapedRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getRecipeWidth(), recipe.getRecipeHeight(),
                recipe.getIngredients(), recipe.getResultItem(RegistryAccess.EMPTY));
    }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registries) {
        ItemStack result = super.assemble(grid, registries);
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack input = grid.getItem(slot);
            if (input.getItem() instanceof IUpgradeItem<?>) {
                result.setTag(input.hasTag() ? input.getTag().copy() : null);
                break;
            }
        }
        if (result.getItem() instanceof org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeItem) {
            for (int slot = 0; slot < grid.getContainerSize(); slot++) {
                if (org.brahypno.sophisticated_smeltery.upgrade.TinkerStationAppearance.accepts(result, grid.getItem(slot)))
                    return org.brahypno.sophisticated_smeltery.upgrade.TinkerStationAppearance.bind(result, grid.getItem(slot));
            }
        }
        return result;
    }
    @Override public RecipeSerializer<?> getSerializer() { return SophisticatedSmeltery.CASTING_TIER.get(); }
    public static final class Serializer implements RecipeSerializer<CastingTierRecipe> {
        private final ShapedRecipe.Serializer shaped = new ShapedRecipe.Serializer();
        @Override public CastingTierRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new CastingTierRecipe(shaped.fromJson(id, json));
        }
        @Override public CastingTierRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new CastingTierRecipe(shaped.fromNetwork(id, buffer));
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, CastingTierRecipe recipe) {
            shaped.toNetwork(buffer, recipe);
        }
    }
}
