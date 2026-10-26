package org.brahypno.sophisticated_smeltery.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;

/** A normal crafting recipe for recipe-book/JEI discovery, with exact rebinding remainders. */
public final class VisibleControllerBindingRecipe extends ShapelessRecipe {
    private final ItemStack controller;
    private final ItemStack upgrade;
    private final ControllerBindingRecipe binding;

    public VisibleControllerBindingRecipe(ResourceLocation id, ItemStack controller, ItemStack upgrade) {
        super(id, SophisticatedSmeltery.id("controller_binding").toString(), CraftingBookCategory.MISC,
                ControllerBinding.bind(upgrade, controller),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(upgrade), Ingredient.of(controller)));
        this.controller = controller;
        this.upgrade = upgrade;
        binding = new ControllerBindingRecipe(id, CraftingBookCategory.MISC);
    }

    @Override public boolean matches(CraftingContainer grid, Level level) {
        return super.matches(grid, level) && binding.matches(grid, level);
    }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registries) { return binding.assemble(grid, registries); }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) { return binding.getRemainingItems(grid); }
    @Override public RecipeSerializer<?> getSerializer() { return SophisticatedSmeltery.VISIBLE_BINDING.get(); }

    public static final class Serializer implements RecipeSerializer<VisibleControllerBindingRecipe> {
        @Override public VisibleControllerBindingRecipe fromJson(ResourceLocation id, JsonObject json) {
            ResourceLocation item = new ResourceLocation(GsonHelper.getAsString(json, "controller"));
            if (!ForgeRegistries.ITEMS.containsKey(item)) throw new IllegalArgumentException("Unknown controller " + item);
            ResourceLocation upgrade = new ResourceLocation(GsonHelper.getAsString(json, "upgrade"));
            if (!ForgeRegistries.ITEMS.containsKey(upgrade)) throw new IllegalArgumentException("Unknown upgrade " + upgrade);
            return new VisibleControllerBindingRecipe(id, new ItemStack(ForgeRegistries.ITEMS.getValue(item)),
                    new ItemStack(ForgeRegistries.ITEMS.getValue(upgrade)));
        }
        @Override public VisibleControllerBindingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new VisibleControllerBindingRecipe(id, buffer.readItem(), buffer.readItem());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, VisibleControllerBindingRecipe recipe) {
            buffer.writeItem(recipe.controller);
            buffer.writeItem(recipe.upgrade);
        }
    }
}
