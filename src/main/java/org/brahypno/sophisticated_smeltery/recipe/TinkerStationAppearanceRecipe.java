package org.brahypno.sophisticated_smeltery.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationAppearance;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeItem;

/** Replaces the workstation appearance without clearing tools, ingredients or settings. */
public final class TinkerStationAppearanceRecipe extends CustomRecipe {
    public TinkerStationAppearanceRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }

    private int[] ingredients(CraftingContainer grid) {
        int upgrade = -1, workstation = -1;
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack stack = grid.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof TinkerStationUpgradeItem && upgrade == -1) upgrade = slot;
            else if (workstation == -1) workstation = slot;
            else return null;
        }
        return upgrade >= 0 && workstation >= 0 && TinkerStationAppearance.accepts(grid.getItem(upgrade), grid.getItem(workstation))
                ? new int[]{upgrade, workstation} : null;
    }

    @Override public boolean matches(CraftingContainer grid, Level level) { return ingredients(grid) != null; }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registries) {
        int[] slots = ingredients(grid);
        return slots == null ? ItemStack.EMPTY : TinkerStationAppearance.bind(grid.getItem(slots[0]), grid.getItem(slots[1]));
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);
        int[] slots = ingredients(grid);
        if (slots != null) remaining.set(slots[0], TinkerStationAppearance.bound(grid.getItem(slots[0])));
        return remaining;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return SophisticatedSmeltery.WORKSTATION_APPEARANCE.get(); }
}
