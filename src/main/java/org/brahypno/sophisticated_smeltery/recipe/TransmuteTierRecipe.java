package org.brahypno.sophisticated_smeltery.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;

/** Keep the complete standard furnace while adding the two native switches. */
public final class TransmuteTierRecipe extends ShapedRecipe {
    public TransmuteTierRecipe(ShapedRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getRecipeWidth(), recipe.getRecipeHeight(), recipe.getIngredients(),
                ControllerBinding.bind(recipe.getResultItem(RegistryAccess.EMPTY),
                        new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("esotericism_tinker", "transmute_controller")))));
    }

    @Override public boolean matches(CraftingContainer grid, Level level) {
        if (!super.matches(grid, level)) return false;
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack input = grid.getItem(slot);
            if (input.is(SophisticatedSmeltery.MELTING_UPGRADE.get()))
                return SophisticatedSmeltery.ADVANCED_TRANSMUTE_UPGRADE.get().acceptsController(ControllerBinding.getController(input));
        }
        return false;
    }

    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registries) {
        ItemStack result = super.assemble(grid, registries);
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack input = grid.getItem(slot);
            if (input.is(SophisticatedSmeltery.MELTING_UPGRADE.get())) {
                result.setTag(input.hasTag() ? input.getTag().copy() : null);
                break;
            }
        }
        return result;
    }

    @Override public RecipeSerializer<?> getSerializer() { return SophisticatedSmeltery.TRANSMUTE_TIER.get(); }
    public static final class Serializer implements RecipeSerializer<TransmuteTierRecipe> {
        private final ShapedRecipe.Serializer shaped = new ShapedRecipe.Serializer();
        @Override public TransmuteTierRecipe fromJson(ResourceLocation id, JsonObject json) { return new TransmuteTierRecipe(shaped.fromJson(id, json)); }
        @Override public TransmuteTierRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) { return new TransmuteTierRecipe(shaped.fromNetwork(id, buffer)); }
        @Override public void toNetwork(FriendlyByteBuf buffer, TransmuteTierRecipe recipe) { shaped.toNetwork(buffer, recipe); }
    }
}
