package org.brahypno.sophisticated_smeltery;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.brahypno.sophisticated_smeltery.recipe.CastingTierRecipe;
import org.brahypno.sophisticated_smeltery.recipe.TinkerStationAppearanceRecipe;
import org.brahypno.sophisticated_smeltery.recipe.UpgradeClearingRecipe;
import org.brahypno.sophisticated_smeltery.upgrade.ModifierWorktableUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.ModifierWorktableUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationAppearance;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.table.ModifierWorktableBlockEntity;
import slimeknights.tconstruct.tools.TinkerTools;

@PrefixGameTestTemplate(false)
public final class ModifierWorktableUpgradeGameTests {
    private record Host(IStorageWrapper storage, String prefix) { }
    private static List<Host> hosts(GameTestHelper helper) {
        var result = new ArrayList<Host>();
        if (ModList.get().isLoaded("sophisticatedbackpacks")) {
            var storage = new net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper(
                    new ItemStack(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.NETHERITE_BACKPACK.get()));
            storage.getInventoryHandler(); result.add(new Host(storage, ""));
        }
        if (ModList.get().isLoaded("sophisticatedstorage")) {
            helper.setBlock(new BlockPos(1, 1, 1), net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.NETHERITE_CHEST.get());
            var block = (net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity)helper.getBlockEntity(new BlockPos(1, 1, 1));
            result.add(new Host(block.getStorageWrapper(), "storage_"));
        }
        return result;
    }
    private static ItemStack stack(String id) { return new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id))); }
    private static ModifierWorktableUpgradeWrapper wrapper(GameTestHelper helper, Host host) {
        helper.assertTrue(host.storage.getUpgradeHandler().insertItem(0, stack("sophisticated_smeltery:" + host.prefix + "modifier_worktable_upgrade"), false).isEmpty(), "Host accepts worktable upgrade");
        var wrapper = (ModifierWorktableUpgradeWrapper)host.storage.getUpgradeHandler().getSlotWrappers().get(0);
        wrapper.bind(helper.getLevel()); return wrapper;
    }
    static ItemStack tool() {
        var item = TinkerTools.pickaxe.get();
        var iron = MaterialVariant.of(MaterialVariantId.parse("tconstruct:iron"));
        var tool = ToolStack.createTool(item, item.getToolDefinition(), new MaterialNBT(List.of(iron, iron, iron)));
        tool.addModifier(new ModifierId("tconstruct", "haste"), 1);
        tool.addModifier(new ModifierId("tconstruct", "diamond"), 1);
        return tool.createStack();
    }
    private static ModifierWorktableBlockEntity baseline(GameTestHelper helper, ModifierWorktableUpgradeWrapper wrapper) {
        var tile = new ModifierWorktableBlockEntity(BlockPos.ZERO, TinkerTables.modifierWorktable.get().defaultBlockState());
        tile.setLevel(helper.getLevel());
        for (int slot = 0; slot < 3; slot++) tile.setItem(slot, wrapper.inventory().getStackInSlot(slot).copy());
        return tile;
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void nativeRemovalExtractionSortingAndFluidContainers(GameTestHelper helper) {
        for (var host : hosts(helper)) {
            var wrapper = wrapper(helper, host);
            for (String operation : List.of("remove", "extract", "sort", "hide", "show", "book")) {
                ItemStack input = tool();
                if (operation.equals("show")) {
                    wrapper.inventory().setStackInSlot(0, input); wrapper.setInput(0, stack("tconstruct:sky_slime_bottle")); wrapper.setInput(1, ItemStack.EMPTY);
                    wrapper.select(0); input = wrapper.result();
                } else if (operation.equals("book")) {
                    input = new ItemStack(Items.ENCHANTED_BOOK);
                    net.minecraft.world.item.EnchantedBookItem.addEnchantment(input, new net.minecraft.world.item.enchantment.EnchantmentInstance(Enchantments.BLOCK_EFFICIENCY, 1));
                }
                wrapper.inventory().setStackInSlot(0, input);
                wrapper.setInput(0, switch (operation) {
                    case "remove" -> new ItemStack(Items.WET_SPONGE, 3);
                    case "extract", "book" -> stack("tconstruct:sky_slime_crystal");
                    case "sort" -> new ItemStack(Items.COMPASS);
                    case "hide" -> stack("tconstruct:sky_slime_bottle");
                    default -> new ItemStack(Items.MILK_BUCKET);
                });
                wrapper.setInput(1, operation.equals("extract") ? new ItemStack(Items.WET_SPONGE, 2)
                        : operation.equals("book") ? new ItemStack(Items.LAPIS_LAZULI, 8) : ItemStack.EMPTY);
                var tile = baseline(helper, wrapper);
                helper.assertTrue(wrapper.recipe() != null && wrapper.recipe().getId().equals(tile.getCurrentRecipe().getId()), "Same native recipe for " + operation);
                helper.assertTrue(!wrapper.options().isEmpty() && wrapper.options().equals(tile.getCurrentButtons()), "Same native modifier choices for " + operation);
                wrapper.select(0); tile.selectModifier(0);
                var player = helper.makeMockPlayer(); var nativePlayer = helper.makeMockPlayer();
                var expected = tile.calcResult(nativePlayer).copy();
                helper.assertTrue(!expected.isEmpty() && ItemStack.matches(expected, wrapper.result()), "Exact native preview for " + operation);
                var container = new ModifierWorktableUpgradeContainer(player, 0, wrapper, SophisticatedSmeltery.MODIFIER_WORKTABLE_CONTAINER);
                var output = container.resultSlot();
                var before = wrapper.inventory().serializeNBT(); output.getItem(); output.getItem();
                helper.assertTrue(before.equals(wrapper.inventory().serializeNBT()), "Preview reads consume nothing");
                var taken = output.remove(1); output.onTake(player, taken);
                tile.onCraft(nativePlayer, expected, expected.getCount());
                for (int slot = 0; slot < 3; slot++) helper.assertTrue(ItemStack.matches(wrapper.inventory().getStackInSlot(slot), tile.getItem(slot)), "Native input consumption for " + operation + "/" + slot);
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) helper.assertTrue(ItemStack.matches(player.getInventory().getItem(slot), nativePlayer.getInventory().getItem(slot)), "Native leftovers for " + operation);
                helper.assertTrue(wrapper.selected() == -1 && output.getItem().isEmpty() && !output.mayPlace(input) && !container.allowsPickupAll(output), "Operation clears selection and result");
            }
        }
        helper.succeed();
    }
    private static TransientCraftingContainer grid() {
        return new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(Player player) { return true; }
        }, 3, 3);
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void realHostShiftCraftingFullInventoriesAndPlayerEquipment(GameTestHelper helper) {
        for (var host : hosts(helper)) {
            wrapper(helper, host);
            var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "worktable-test"));
            var channel = new io.netty.channel.embedded.EmbeddedChannel(player.connection.connection);
            StorageContainerMenuBase<?> menu = host.prefix.isEmpty() ? BackpackMenu.create(player, host.storage.getWrappedStorageStack())
                    : new net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu(1, player, helper.absolutePos(new BlockPos(1, 1, 1)));
            player.containerMenu = menu; menu.setOpenTabId(0);
            var container = (ModifierWorktableUpgradeContainer)menu.getUpgradeContainers().get(0);
            var worktable = container.getUpgradeWrapper();
            worktable.inventory().setStackInSlot(0, tool()); worktable.setInput(0, new ItemStack(Items.WET_SPONGE, 3));
            var data = new CompoundTag(); data.putInt("modifier", 0); data.putBoolean("destination", true); container.handleMessage(data);
            ItemStack expected = worktable.result();
            helper.assertTrue(!expected.isEmpty(), "Real installed menu offers selected recipe");
            menu.quickMoveStack(player, container.resultSlot().index);
            int stored = 0;
            var hostInventory = menu.getStorageWrapper().getInventoryHandler();
            for (int slot = 0; slot < hostInventory.getSlots(); slot++) {
                var value = hostInventory.getStackInSlot(slot);
                if (ItemStack.isSameItemSameTags(value, expected)) stored += value.getCount();
            }
            helper.assertTrue(stored == expected.getCount() && worktable.getTinkerableStack().isEmpty() && worktable.getInput(0).getCount() == 2, "Shift takes one complete result into chosen host and consumes once");
            helper.assertTrue(player.getInventory().countItem(Items.SPONGE) == 1, "Native leftover reaches player inventory");
            worktable.inventory().setStackInSlot(0, tool()); worktable.select(0);
            for (int slot = 0; slot < hostInventory.getSlots(); slot++) hostInventory.setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
            for (int slot = 1; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
            player.getInventory().setItem(39, new ItemStack(Items.DIAMOND_HELMET)); player.getInventory().setItem(40, new ItemStack(Items.SHIELD));
            var before = worktable.inventory().serializeNBT();
            helper.assertTrue(menu.quickMoveStack(player, container.resultSlot().index).isEmpty() && before.equals(worktable.inventory().serializeNBT()), "Full destinations leave preview and all ingredients intact");
            for (int slot = 0; slot < 3; slot++) worktable.inventory().setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
            menu.quickMoveStack(player, 0);
            helper.assertTrue(player.getOffhandItem().is(Items.SHIELD), "Automatic ingredient transfer excludes equipment aliases");
            var helmet = container.equipmentSlots().get(0);
            helmet.getItem().enchant(Enchantments.BINDING_CURSE, 1); player.getAbilities().instabuild = false;
            helper.assertTrue(!helmet.mayPickup(player) && !helmet.mayPlace(new ItemStack(Items.DIRT)), "Native armor filtering and binding curse apply");
            menu.removed(player);
            helper.assertTrue(player.getOffhandItem().is(Items.SHIELD) && player.getInventory().getItem(39).is(Items.DIAMOND_HELMET), "Closing either host leaves equipped items on player");
            player.containerMenu = player.inventoryMenu; channel.finishAndReleaseAll();
        }
        helper.succeed();
    }
    private static final class BackpackMenu {
        private static StorageContainerMenuBase<?> create(Player player, ItemStack stack) {
            player.getInventory().setItem(0, stack);
            return new net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer(1, player,
                    new net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext.Item(
                            net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider.MAIN_INVENTORY, 0));
        }
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void craftingAppearancePersistenceAndClearing(GameTestHelper helper) {
        for (var host : hosts(helper)) {
            var grid = grid();
            ItemStack table = slimeknights.mantle.util.RetexturedHelper.setTexture(new ItemStack(TinkerTables.modifierWorktable), net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS);
            grid.setItem(1, table); grid.setItem(3, new ItemStack(Items.IRON_INGOT)); grid.setItem(5, new ItemStack(Items.IRON_INGOT));
            grid.setItem(4, stack((host.prefix.isEmpty() ? "sophisticatedbackpacks" : "sophisticatedstorage") + ":upgrade_base")); grid.setItem(7, new ItemStack(Items.CHEST));
            var recipe = (CastingTierRecipe)helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id(host.prefix + "modifier_worktable_upgrade")).orElseThrow();
            helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Native worktable upgrade recipe");
            var upgrade = recipe.assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(ItemStack.matches(table, TinkerStationAppearance.bound(upgrade)), "Crafting retains native wood skin");
            var wrapper = new ModifierWorktableUpgradeWrapper(host.storage, upgrade, ignored -> {}); wrapper.bind(helper.getLevel());
            wrapper.inventory().setStackInSlot(0, tool()); wrapper.setInput(0, new ItemStack(Items.WET_SPONGE, 5)); wrapper.select(0); wrapper.setShiftIntoStorage(true);
            var restored = new ModifierWorktableUpgradeWrapper(host.storage, wrapper.getUpgradeStack().copy(), ignored -> {}); restored.bind(helper.getLevel());
            helper.assertTrue(restored.shiftIntoStorage(), "Destination survives reload");
            helper.assertTrue(restored.getInput(0).getCount() == 5, "Material count survives reload: " + restored.inventory().serializeNBT());
            // Tinkers' item loading normalizes the implicit, unbroken state into tic_broken=false.
            ItemStack loadedPreview = ItemStack.of(wrapper.result().save(new CompoundTag()));
            helper.assertTrue(ItemStack.matches(loadedPreview, restored.result()), "Selected preview survives the native item-loading normalization");
            var clearing = new UpgradeClearingRecipe(SophisticatedSmeltery.id("test_clear_worktable"), net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
            grid.clearContent(); grid.setItem(0, wrapper.getUpgradeStack());
            helper.assertTrue(!clearing.matches(grid, helper.getLevel()), "Cannot clear a worktable containing items");
            var appearance = new TinkerStationAppearanceRecipe(SophisticatedSmeltery.id("test_worktable_skin"), net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
            grid.setItem(1, new ItemStack(TinkerTables.modifierWorktable));
            helper.assertTrue(appearance.matches(grid, helper.getLevel()), "Filled worktable can change skin");
            var rebound = appearance.assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(rebound.getTag().getCompound("modifierWorktableInventory").equals(wrapper.getUpgradeStack().getTag().getCompound("modifierWorktableInventory"))
                    && ItemStack.matches(table, appearance.getRemainingItems(grid).get(0)), "Rebinding keeps contents and returns previous table");
            for (int slot = 0; slot < 3; slot++) wrapper.inventory().setStackInSlot(slot, ItemStack.EMPTY);
            grid.clearContent(); grid.setItem(0, wrapper.getUpgradeStack());
            helper.assertTrue(clearing.matches(grid, helper.getLevel()) && ItemStack.matches(table, clearing.getRemainingItems(grid).get(0)), "Empty clearing returns bound native table");
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void invalidSelectionInputChangesAndRecipeReload(GameTestHelper helper) {
        for (var host : hosts(helper)) {
            var wrapper = wrapper(helper, host);
            wrapper.inventory().setStackInSlot(0, tool()); wrapper.setInput(0, new ItemStack(Items.WET_SPONGE));
            wrapper.select(0); var expected = wrapper.result();
            wrapper.select(Integer.MAX_VALUE); wrapper.select(-2);
            helper.assertTrue(wrapper.selected() == 0 && ItemStack.matches(wrapper.result(), expected), "Out-of-range selection is ignored");
            wrapper.setInput(0, new ItemStack(Items.DIRT));
            helper.assertTrue(wrapper.result().isEmpty() && wrapper.options().isEmpty() && wrapper.selected() == -1, "Input replacement invalidates result");
            wrapper.setInput(0, new ItemStack(Items.WET_SPONGE)); wrapper.select(0);
            ModifierWorktableUpgradeWrapper.invalidateRecipes();
            helper.assertTrue(wrapper.result().isEmpty() && wrapper.selected() == -1 && !wrapper.options().isEmpty(), "Recipe reload requires a new selection");
            wrapper.select(0);
            helper.assertTrue(ItemStack.matches(wrapper.result(), expected), "New selection uses reloaded recipes");
        }
        helper.succeed();
    }
}
