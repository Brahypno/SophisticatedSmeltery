package org.brahypno.sophisticated_smeltery.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeClearRecipe;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.PartBuilderUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.PartBuilderUpgradeWrapper;

/** Core's clearing rules, with molten-content checks and controller returns for melting upgrades. */
public final class UpgradeClearingRecipe extends UpgradeClearRecipe {
    public UpgradeClearingRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }

    @Override public boolean matches(CraftingContainer grid, Level level) {
        if (!super.matches(grid, level)) return false;
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack stack = grid.getItem(slot);
            if (stack.getItem() instanceof MeltingUpgradeItem && !ControllerBinding.cleanForBinding(stack)) return false;
            if (stack.getItem() instanceof PartBuilderUpgradeItem
                    && !PartBuilderUpgradeWrapper.cleanForClearing(stack)) return false;
            if (stack.getItem() instanceof org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeItem
                    && !org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeWrapper.cleanForClearing(stack)) return false;
            if (stack.getItem() instanceof AlloyerUpgradeItem && !AlloyerUpgradeWrapper.cleanForClearing(stack)) return false;
        }
        return true;
    }

    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(grid);
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack stack = grid.getItem(slot);
            if (stack.getItem() instanceof MeltingUpgradeItem) remaining.set(slot, ControllerBinding.getController(stack));
        }
        return remaining;
    }

    @Override public RecipeSerializer<?> getSerializer() { return SophisticatedSmeltery.UPGRADE_CLEAR.get(); }
}
