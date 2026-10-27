package org.brahypno.sophisticated_smeltery;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import org.brahypno.sophisticated_smeltery.upgrade.PartBuilderUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.PartBuilderUpgradeWrapper;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.partbuilder.ItemPartRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;

@PrefixGameTestTemplate(false)
public final class PartBuilderUpgradeGameTests {
    private static PartBuilderUpgradeWrapper upgrade(GameTestHelper helper) {
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        helper.assertTrue(storage.getUpgradeHandler().insertItem(0, new ItemStack(SophisticatedSmeltery.PART_BUILDER_UPGRADE.get()), false).isEmpty(), "Install through the normal upgrade handler");
        var wrapper = (PartBuilderUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
        wrapper.bind(helper.getLevel());
        return wrapper;
    }
    private static PartBuilderBlockEntity nativeTable(GameTestHelper helper) {
        var tile = new PartBuilderBlockEntity(BlockPos.ZERO, TinkerTables.partBuilder.get().defaultBlockState());
        tile.setLevel(helper.getLevel());
        return tile;
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void nativePatternListsAndResultsMatchAcrossMaterials(GameTestHelper helper) {
        var wrapper = upgrade(helper);
        var nativeTable = nativeTable(helper);
        for (var material : List.of(Items.OAK_LOG, Items.STICK, Items.COBBLESTONE, Items.COPPER_INGOT, Items.DIRT)) {
            wrapper.inventory().setStackInSlot(0, new ItemStack(material, 16));
            wrapper.inventory().setStackInSlot(1, new ItemStack(TinkerTables.pattern, 16));
            nativeTable.setItem(0, new ItemStack(material, 16));
            nativeTable.setItem(1, new ItemStack(TinkerTables.pattern, 16));
            helper.assertTrue(wrapper.patterns().equals(nativeTable.getSortedButtons()), "Native pattern list and ordering for " + material);
            for (var pattern : wrapper.patterns()) {
                wrapper.select(pattern);
                nativeTable.selectRecipe(pattern);
                helper.assertTrue(ItemStack.matches(wrapper.result(), nativeTable.calcResult(null)), "Native result and material data for " + pattern + " / " + material);
            }
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void takingResultsConsumesNativeInputsAndReturnsLeftovers(GameTestHelper helper) {
        var wrapper = upgrade(helper);
        var nativeTable = nativeTable(helper);
        var player = helper.makeMockPlayer();
        var baseline = helper.makeMockPlayer();
        var pattern = new Pattern("tconstruct:pick_head");
        wrapper.inventory().setStackInSlot(0, new ItemStack(Items.OAK_LOG, 16));
        wrapper.inventory().setStackInSlot(1, new ItemStack(TinkerTables.pattern, 8));
        wrapper.select(pattern);
        nativeTable.setItem(0, new ItemStack(Items.OAK_LOG, 16));
        nativeTable.setItem(1, new ItemStack(TinkerTables.pattern, 8));
        nativeTable.selectRecipe(pattern);
        var container = new PartBuilderUpgradeContainer(player, 0, wrapper, SophisticatedSmeltery.PART_BUILDER_CONTAINER);
        var output = container.getSlots().get(2);
        for (int craft = 0; craft < 3; craft++) {
            ItemStack expected = nativeTable.calcResult(baseline);
            helper.assertTrue(!expected.isEmpty() && ItemStack.matches(output.getItem(), expected), "Result preview matches native table");
            int before = wrapper.getStack().getCount();
            output.getItem(); output.getItem();
            helper.assertTrue(wrapper.getStack().getCount() == before, "Previewing never consumes materials");
            var taken = output.remove(1);
            output.onTake(player, taken);
            nativeTable.onCraft(baseline, expected, expected.getCount());
            helper.assertTrue(ItemStack.matches(wrapper.getStack(), nativeTable.getItem(0))
                    && ItemStack.matches(wrapper.getPatternStack(), nativeTable.getItem(1)), "Taking consumes exactly the native material and pattern amounts");
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
                helper.assertTrue(ItemStack.matches(player.getInventory().getItem(slot), baseline.getInventory().getItem(slot)), "Native leftover material and containers are returned");
        }
        helper.assertTrue(!container.allowsPickupAll(output) && !output.mayPlace(new ItemStack(Items.STICK)), "Result cannot accept input or join double-click collection");
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, batch = "part_builder_recipes")
    public static void reloadSupportsReusablePatternsAndMultipleResults(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        var wrapper = upgrade(helper);
        var pattern = new Pattern("sophisticated_smeltery:test_parts");
        ItemStack cast = new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("tconstruct:pick_head_cast")));
        wrapper.inventory().setStackInSlot(0, new ItemStack(Items.STICK, 16));
        wrapper.inventory().setStackInSlot(1, cast.copy());
        wrapper.patterns();
        try {
            var recipes = new ArrayList<>(original);
            recipes.add(new ItemPartRecipe(SophisticatedSmeltery.id("test_parts"), MaterialVariantId.parse("tconstruct:wood"), pattern,
                    Ingredient.of(cast), 1, ItemOutput.fromStack(new ItemStack(Items.STICK, 4))));
            manager.replaceRecipes(recipes);
            PartBuilderUpgradeWrapper.invalidateRecipes();
            helper.assertTrue(wrapper.patterns().contains(pattern), "Reload refreshes an already open pattern list");
            wrapper.select(pattern);
            var container = new PartBuilderUpgradeContainer(helper.makeMockPlayer(), 0, wrapper, SophisticatedSmeltery.PART_BUILDER_CONTAINER);
            var output = container.getSlots().get(2);
            helper.assertTrue(output.getItem().getCount() == 4, "Custom recipe output count is preserved");
            var taken = output.remove(1);
            output.onTake(container.getPlayer(), taken);
            helper.assertTrue(taken.getCount() == 4 && wrapper.getPatternStack().getCount() == 1, "Native whole result pickup preserves reusable patterns");
            output.getItem();
            output.set(new ItemStack(Items.STICK, 2));
            output.onTake(container.getPlayer(), ItemStack.EMPTY);
            int returned = 0;
            for (int slot = 0; slot < container.getPlayer().getInventory().getContainerSize(); slot++) {
                var stack = container.getPlayer().getInventory().getItem(slot);
                if (stack.is(Items.STICK)) returned += stack.getCount();
            }
            helper.assertTrue(returned == 2 && wrapper.getStack().getCount() == 12 && wrapper.getPatternStack().getCount() == 1,
                    "Partial quick transfer returns the two untransferred results and consumes exactly one more recipe");
            manager.replaceRecipes(original);
            PartBuilderUpgradeWrapper.invalidateRecipes();
            helper.assertTrue(!wrapper.patterns().contains(pattern) && output.getItem().isEmpty(), "Removed recipes cannot leave stale craftable results");
        } finally { manager.replaceRecipes(original); PartBuilderUpgradeWrapper.invalidateRecipes(); }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void stateAndDestinationPersistAndInvalidSelectionIsIgnored(GameTestHelper helper) {
        var wrapper = upgrade(helper);
        wrapper.inventory().setStackInSlot(0, new ItemStack(Items.OAK_LOG, 16));
        wrapper.inventory().setStackInSlot(1, new ItemStack(TinkerTables.pattern, 8));
        wrapper.select(new Pattern("tconstruct:pick_head"));
        wrapper.setShiftIntoStorage(true);
        var restored = new PartBuilderUpgradeWrapper(new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get())), wrapper.getUpgradeStack().copy(), ignored -> {});
        restored.bind(helper.getLevel());
        helper.assertTrue(ItemStack.matches(restored.result(), wrapper.result()) && restored.shiftIntoStorage()
                && ItemStack.matches(restored.getStack(), wrapper.getStack()) && ItemStack.matches(restored.getPatternStack(), wrapper.getPatternStack()), "Selection, inputs and destination survive reload");
        var container = new PartBuilderUpgradeContainer(helper.makeMockPlayer(), 0, restored, SophisticatedSmeltery.PART_BUILDER_CONTAINER);
        var invalid = new CompoundTag(); invalid.putString("pattern", "sophisticated_smeltery:missing"); container.handleMessage(invalid);
        helper.assertTrue(restored.selected().equals(wrapper.selected()), "Unavailable patterns cannot replace a valid selection");
        var clearing = new org.brahypno.sophisticated_smeltery.recipe.UpgradeClearingRecipe(SophisticatedSmeltery.id("test_clear"), net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        var menu = new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        };
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 3, 3);
        grid.setItem(0, restored.getUpgradeStack());
        helper.assertTrue(!clearing.matches(grid, helper.getLevel()), "The clearing recipe cannot erase input items");
        restored.inventory().setStackInSlot(0, ItemStack.EMPTY); restored.inventory().setStackInSlot(1, ItemStack.EMPTY);
        helper.assertTrue(clearing.matches(grid, helper.getLevel()), "The clearing recipe accepts an empty part builder upgrade");
        helper.succeed();
    }
}

