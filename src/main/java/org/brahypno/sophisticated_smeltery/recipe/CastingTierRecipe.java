package org.brahypno.sophisticated_smeltery.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;

/** Native upgrade-state transfer, also visible to the vanilla recipe book. */
public final class CastingTierRecipe extends UpgradeNextTierRecipe {
    public CastingTierRecipe(ShapedRecipe recipe) { super(recipe); }
    @Override public boolean isSpecial() { return false; }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registries) {
        // The native implementation shares the input tag; give the crafted item its own copy.
        return super.assemble(grid, registries).copy();
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
            shaped.toNetwork(buffer, recipe.getCompose());
        }
    }
}
