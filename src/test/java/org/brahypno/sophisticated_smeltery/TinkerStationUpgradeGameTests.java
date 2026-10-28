package org.brahypno.sophisticated_smeltery;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import org.brahypno.sophisticated_smeltery.recipe.CastingTierRecipe;
import org.brahypno.sophisticated_smeltery.recipe.UpgradeClearingRecipe;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationAppearance;
import slimeknights.mantle.util.RetexturedHelper;
import net.minecraft.world.level.block.Blocks;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tools.TinkerToolParts;

@PrefixGameTestTemplate(false)
public final class TinkerStationUpgradeGameTests {
    private record Host(IStorageWrapper storage, String prefix) {}
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
    private static ItemStack stack(String path) { return new ItemStack(ForgeRegistries.ITEMS.getValue(SophisticatedSmeltery.id(path))); }
    private static TinkerStationUpgradeWrapper wrapper(GameTestHelper helper, Host host, boolean advanced) {
        var stack = stack(host.prefix + (advanced ? "tinkers_anvil_upgrade" : "tinker_station_upgrade"));
        host.storage.getUpgradeHandler().setStackInSlot(0, ItemStack.EMPTY);
        helper.assertTrue(host.storage.getUpgradeHandler().insertItem(0, stack, false).isEmpty(), "Native host accepts its workstation upgrade");
        var wrapper = (TinkerStationUpgradeWrapper)host.storage.getUpgradeHandler().getSlotWrappers().get(0);
        wrapper.bind(helper.getLevel()); return wrapper;
    }
    private static ToolBuildingRecipe building(GameTestHelper helper, String tool) {
        return helper.getLevel().getRecipeManager().getAllRecipesFor(TinkerRecipeTypes.TINKER_STATION.get()).stream()
                .filter(ToolBuildingRecipe.class::isInstance).map(ToolBuildingRecipe.class::cast)
                .filter(recipe -> ForgeRegistries.ITEMS.getKey(recipe.getOutput().asItem()).equals(new ResourceLocation("tconstruct", tool)))
                .findFirst().orElseThrow();
    }
    private static ItemStack part(net.minecraft.world.level.ItemLike item, String material) {
        return IMaterialItem.withMaterial(new ItemStack(item), MaterialVariantId.parse("tconstruct:" + material));
    }
    private static void fill(TinkerStationUpgradeWrapper wrapper, ToolBuildingRecipe recipe) {
        for (int slot = 0; slot < wrapper.inventory().getSlots(); slot++) wrapper.inventory().setStackInSlot(slot, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(recipe.getToolParts().size(), wrapper.getInputCount()); i++)
            wrapper.setInput(i, part(recipe.getToolParts().get(i), "iron"));
    }
    private static TinkerStationBlockEntity baseline(GameTestHelper helper, TinkerStationUpgradeWrapper wrapper) {
        var tile = new TinkerStationBlockEntity(BlockPos.ZERO, (wrapper.isAdvanced() ? TinkerTables.tinkersAnvil : TinkerTables.tinkerStation).get().defaultBlockState());
        tile.setLevel(helper.getLevel());
        for (int i = 0; i < wrapper.inventory().getSlots(); i++) tile.setItem(i, wrapper.inventory().getStackInSlot(i).copy());
        tile.setItemName(wrapper.itemName()); return tile;
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void tiersBuildNativeToolsAndKeepLargeToolsOnAnvil(GameTestHelper helper) {
        for (var host : hosts(helper)) for (boolean advanced : List.of(false, true)) {
            var wrapper = wrapper(helper, host, advanced);
            helper.assertTrue(wrapper.getInputCount() == (advanced ? 5 : 3), "Native station tier slot count");
            for (String tool : List.of("pickaxe", "sledge_hammer")) {
                var recipe = building(helper, tool); fill(wrapper, recipe);
                var tile = baseline(helper, wrapper);
                var expected = tile.calcResult(null);
                helper.assertTrue(ItemStack.matches(expected, wrapper.result()), "Native build preview for " + tool + " / " + host.prefix);
                helper.assertTrue(!wrapper.result().isEmpty() == (advanced || tool.equals("pickaxe")), "Large tools require the anvil tier");
            }
            helper.assertTrue(wrapper.layouts().stream().allMatch(layout -> layout.getInputCount() <= wrapper.getInputCount()), "Picker offers only supported layouts");
            var original = wrapper.layout().getName();
            wrapper.select(new ResourceLocation("sophisticated_smeltery:missing"));
            helper.assertTrue(wrapper.layout().getName().equals(original), "Invalid layout messages cannot change selection");
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void playerEquipmentUsesNativeRulesAndStaysOutsideRecipeInventory(GameTestHelper helper) {
        var armor = List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        for (var host : hosts(helper)) for (boolean advanced : List.of(false, true)) {
            wrapper(helper, host, advanced);
            var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "equipment-test"));
            var channel = new io.netty.channel.embedded.EmbeddedChannel(player.connection.connection);
            player.getAbilities().instabuild = false;
            net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase<?> menu;
            if (host.prefix.isEmpty()) {
                menu = BackpackMenu.create(player, host.storage.getWrappedStorageStack());
            } else {
                var block = (net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity)helper.getBlockEntity(new BlockPos(1, 1, 1));
                menu = new net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu(1, player, block.getBlockPos());
            }
            player.containerMenu = menu;
            menu.setOpenTabId(0);
            var station = (TinkerStationUpgradeContainer)menu.getUpgradeContainers().get(0);
            helper.assertTrue(station.equipmentSlots().size() == 5, "Both hosts and tiers expose five player equipment slots");
            for (int index = 0; index < 4; index++) {
                var slot = station.equipmentSlots().get(index);
                helper.assertTrue(slot.getMaxStackSize() == 1 && !slot.mayPlace(new ItemStack(Items.DIRT)), "Native armor limit and equipment filter");
                menu.setCarried(new ItemStack(armor.get(index)));
                menu.clicked(slot.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
                helper.assertTrue(menu.getCarried().isEmpty() && player.getInventory().getItem(39 - index).is(armor.get(index)), "Server clicks equip the correct player armor slot");
            }
            var helmet = station.equipmentSlots().get(0);
            helmet.getItem().enchant(net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE, 1);
            menu.clicked(helmet.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            helper.assertTrue(!helmet.mayPickup(player) && menu.getCarried().isEmpty() && helmet.hasItem(), "Binding curse blocks server-side removal");
            var offhand = station.equipmentSlots().get(4);
            menu.setCarried(new ItemStack(Items.SHIELD));
            menu.clicked(offhand.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            helper.assertTrue(player.getOffhandItem().is(Items.SHIELD) && menu.getCarried().isEmpty(), "Offhand is a real player slot");
            offhand.set(ItemStack.EMPTY);
            var stationWrapper = station.getUpgradeWrapper();
            for (int index = 0; index < stationWrapper.inventory().getSlots(); index++) stationWrapper.inventory().setStackInSlot(index, new ItemStack(Items.DIRT, 64));
            menu.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.STICK, 64));
            menu.quickMoveStack(player, 0);
            helper.assertTrue(offhand.getItem().isEmpty(), "Recipe Shift transfer cannot fall through into the offhand slot");
            if (host.prefix.isEmpty()) {
                offhand.set(host.storage.getWrappedStorageStack().copy());
                helper.assertTrue(!offhand.mayPickup(player) && !offhand.mayPlace(new ItemStack(Items.SHIELD)), "The open host cannot be removed or replaced through the equipment alias");
            }
            ItemStack kept = ModList.get().isLoaded("sophisticatedbackpacks")
                    ? BackpackMenu.anotherBackpack() : new ItemStack(Items.SHIELD);
            offhand.set(kept);
            menu.removed(player);
            helper.assertTrue(ItemStack.matches(player.getOffhandItem(), kept) && player.getInventory().getItem(38).is(Items.DIAMOND_CHESTPLATE),
                    "Closing the storage leaves armor and offhand items on the player");
            player.containerMenu = player.inventoryMenu;
            channel.finishAndReleaseAll();
        }
        helper.succeed();
    }
    /** Keep optional menu types out of the verifier's Storage-only path. */
    private static final class BackpackMenu {
        private static net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase<?> create(net.minecraft.world.entity.player.Player player, ItemStack stack) {
            player.getInventory().setItem(0, stack);
            return new net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer(1, player,
                    new net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext.Item(
                            net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider.MAIN_INVENTORY, 0));
        }
        private static ItemStack anotherBackpack() {
            return new ItemStack(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.BACKPACK.get());
        }
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void takingBuildRepairModifyAndSwapMatchesNativeConsumption(GameTestHelper helper) {
        for (var host : hosts(helper)) for (boolean advanced : List.of(false, true)) {
            var wrapper = wrapper(helper, host, advanced);
            fill(wrapper, building(helper, "pickaxe"));
            ItemStack tool = wrapper.result();
            helper.assertTrue(!tool.isEmpty(), "Iron pickaxe can be built");
            for (int operation = 0; operation < 4; operation++) {
                for (int slot = 0; slot < wrapper.inventory().getSlots(); slot++) wrapper.inventory().setStackInSlot(slot, ItemStack.EMPTY);
                if (operation == 0) fill(wrapper, building(helper, "pickaxe"));
                else {
                    ItemStack inputTool = tool.copy();
                    if (operation == 1) ToolStack.from(inputTool).setDamage(80);
                    wrapper.inventory().setStackInSlot(0, inputTool);
                    wrapper.setInput(0, operation == 1 ? new ItemStack(Items.IRON_INGOT, 8)
                            : operation == 2 ? new ItemStack(Items.QUARTZ, 64) : part(TinkerToolParts.pickHead, "copper"));
                }
                wrapper.rename("Portable pick");
                var tile = baseline(helper, wrapper);
                var player = helper.makeMockPlayer(); var nativePlayer = helper.makeMockPlayer();
                ItemStack expected = tile.calcResult(nativePlayer);
                helper.assertTrue(!expected.isEmpty() && ItemStack.matches(expected, wrapper.result()), "Native operation preview " + operation);
                var container = new TinkerStationUpgradeContainer(player, 0, wrapper, SophisticatedSmeltery.TINKER_STATION_CONTAINER);
                var output = container.resultSlot();
                var before = wrapper.inventory().serializeNBT(); output.getItem(); output.getItem();
                helper.assertTrue(before.equals(wrapper.inventory().serializeNBT()), "Reading previews consumes nothing");
                var taken = output.remove(1); output.onTake(player, taken);
                tile.onCraft(nativePlayer, expected, expected.getCount());
                for (int slot = 0; slot < wrapper.inventory().getSlots(); slot++)
                    helper.assertTrue(ItemStack.matches(wrapper.inventory().getStackInSlot(slot), tile.getItem(slot)), "Exact native input consumption for operation " + operation);
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
                    helper.assertTrue(ItemStack.matches(player.getInventory().getItem(slot), nativePlayer.getInventory().getItem(slot)), "Native leftovers for operation " + operation);
                helper.assertTrue(wrapper.itemName().isEmpty() && !output.mayPlace(tool) && !container.allowsPickupAll(output), "Name resets and result slot rejects input and pickup-all");
            }
        }
        helper.succeed();
    }
    private static TransientCraftingContainer grid() {
        return new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        }, 3, 3);
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void tierRecipeAndReloadPreserveInputsSettingsAndNewSlots(GameTestHelper helper) {
        for (var host : hosts(helper)) {
            var wrapper = wrapper(helper, host, false);
            fill(wrapper, building(helper, "pickaxe")); wrapper.select(new ResourceLocation("tconstruct:pickaxe"));
            wrapper.rename("Saved pick"); wrapper.setShiftIntoStorage(true);
            var source = wrapper.getUpgradeStack().copy();
            var grid = grid(); grid.setItem(1, new ItemStack(TinkerTables.tinkersAnvil));
            grid.setItem(3, new ItemStack(Items.GOLD_INGOT)); grid.setItem(4, source); grid.setItem(5, new ItemStack(Items.GOLD_INGOT));
            for (int slot = 6; slot < 9; slot++) grid.setItem(slot, new ItemStack(Items.REDSTONE));
            var id = SophisticatedSmeltery.id(host.prefix + "tinkers_anvil_upgrade");
            var recipe = (CastingTierRecipe)helper.getLevel().getRecipeManager().byKey(id).orElseThrow();
            helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Configured station can be upgraded");
            ItemStack upgraded = recipe.assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(source.getTag().getAllKeys().stream().allMatch(key -> source.getTag().get(key).equals(upgraded.getTag().get(key))), "Tier recipe preserves existing state");
            var restored = new TinkerStationUpgradeWrapper(host.storage, upgraded, ignored -> {}); restored.bind(helper.getLevel());
            helper.assertTrue(restored.inventory().getSlots() == 6 && restored.shiftIntoStorage() && restored.itemName().equals("Saved pick")
                    && restored.layout().getName().equals(wrapper.layout().getName()) && ItemStack.matches(restored.result(), wrapper.result()), "Anvil restores inputs, layout, name and destination with two new slots");
            restored.inventory().setStackInSlot(5, new ItemStack(Items.DIAMOND, 7));
            var reload = new TinkerStationUpgradeWrapper(host.storage, restored.getUpgradeStack().copy(), ignored -> {}); reload.bind(helper.getLevel());
            helper.assertTrue(reload.getInput(4).getCount() == 7, "New anvil slots survive serialization");
            var clearing = new UpgradeClearingRecipe(SophisticatedSmeltery.id("test_clear_station"), net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
            grid.clearContent(); grid.setItem(0, restored.getUpgradeStack());
            helper.assertTrue(!clearing.matches(grid, helper.getLevel()), "Clearing cannot erase workstation contents");
            for (int i = 0; i < restored.inventory().getSlots(); i++) restored.inventory().setStackInSlot(i, ItemStack.EMPTY);
            helper.assertTrue(clearing.matches(grid, helper.getLevel()), "Empty workstation may be cleared");
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void nativeAppearancesSurviveCraftingRebindingAndHostConversion(GameTestHelper helper) {
        for (var host : hosts(helper)) {
            var wood = RetexturedHelper.setTexture(new ItemStack(TinkerTables.tinkerStation), Blocks.CRIMSON_PLANKS);
            var grid = grid(); grid.setItem(1, wood);
            grid.setItem(3, new ItemStack(Items.IRON_INGOT)); grid.setItem(5, new ItemStack(Items.IRON_INGOT));
            grid.setItem(4, new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(
                    host.prefix.isEmpty() ? "sophisticatedbackpacks" : "sophisticatedstorage", "upgrade_base"))));
            grid.setItem(7, new ItemStack(Items.CHEST));
            var recipe = (CastingTierRecipe)helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id(host.prefix + "tinker_station_upgrade")).orElseThrow();
            helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Retextured station matches the original recipe");
            var station = recipe.assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(ItemStack.matches(TinkerStationAppearance.bound(station), wood), "Crafting captures native wood NBT");
            station.getOrCreateTag().putString("tinkerStationName", "Keep this name");
            for (var block : List.of(TinkerTables.tinkersAnvil, TinkerTables.scorchedAnvil)) {
                var anvil = ((IMaterialItem)block.asItem()).withMaterialForDisplay(MaterialVariantId.parse("tconstruct:cobalt"));
                helper.assertTrue(IMaterialItem.getMaterialFromStack(anvil).equals(MaterialVariantId.parse("tconstruct:cobalt")), "Native display interface supplies a real material-bearing anvil");
                grid.clearContent(); grid.setItem(1, anvil); grid.setItem(4, station);
                grid.setItem(3, new ItemStack(Items.GOLD_INGOT)); grid.setItem(5, new ItemStack(Items.GOLD_INGOT));
                for (int slot = 6; slot < 9; slot++) grid.setItem(slot, new ItemStack(Items.REDSTONE));
                recipe = (CastingTierRecipe)helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id(host.prefix + "tinkers_anvil_upgrade")).orElseThrow();
                helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Both native anvils can upgrade a station");
                var upgrade = recipe.assemble(grid, helper.getLevel().registryAccess());
                helper.assertTrue(ItemStack.matches(TinkerStationAppearance.bound(upgrade), anvil)
                        && upgrade.getTag().getString("tinkerStationName").equals("Keep this name"), "Tier crafting captures native material and preserves settings");
                var wrapper = new TinkerStationUpgradeWrapper(host.storage, upgrade, ignored -> {}); wrapper.bind(helper.getLevel());
                helper.assertTrue(wrapper.getInputCount() == 5 && wrapper.layout().getName().equals(ForgeRegistries.BLOCKS.getKey(block.get())), "Native anvil default layout and five inputs");
                fill(wrapper, building(helper, "sledge_hammer"));
                helper.assertTrue(ItemStack.matches(wrapper.result(), baseline(helper, wrapper).calcResult(null)) && !wrapper.result().isEmpty(), "Both anvils build large tools like native stations");
                var before = wrapper.inventory().serializeNBT();
                var skin = RetexturedHelper.setTexture(new ItemStack(block), Blocks.DIAMOND_BLOCK);
                grid.clearContent(); grid.setItem(0, wrapper.getUpgradeStack()); grid.setItem(8, skin);
                var rebind = helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id("workstation_appearance")).orElseThrow();
                var appearanceRecipe = (org.brahypno.sophisticated_smeltery.recipe.TinkerStationAppearanceRecipe)rebind;
                helper.assertTrue(appearanceRecipe.matches(grid, helper.getLevel()), "Appearance can change while tools and parts remain inside");
                upgrade = appearanceRecipe.assemble(grid, helper.getLevel().registryAccess());
                helper.assertTrue(ItemStack.matches(appearanceRecipe.getRemainingItems(grid).get(0), anvil), "Rebinding returns the exact previous anvil");
                var restored = new TinkerStationUpgradeWrapper(host.storage, upgrade, ignored -> {}); restored.bind(helper.getLevel());
                helper.assertTrue(before.equals(restored.inventory().serializeNBT()) && ItemStack.matches(TinkerStationAppearance.bound(upgrade), skin), "Rebinding preserves all inputs and the new skin");
                grid.clearContent(); grid.setItem(0, upgrade);
                var clear = new UpgradeClearingRecipe(SophisticatedSmeltery.id("test_clear_appearance"), net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
                helper.assertTrue(!clear.matches(grid, helper.getLevel()), "Clearing still protects stored parts");
                for (int slot = 0; slot < restored.inventory().getSlots(); slot++) restored.inventory().setStackInSlot(slot, ItemStack.EMPTY);
                grid.setItem(0, restored.getUpgradeStack());
                helper.assertTrue(clear.matches(grid, helper.getLevel()) && ItemStack.matches(clear.getRemainingItems(grid).get(0), skin), "Clearing returns the customized source after contents are removed");
                grid.setItem(8, wood);
                helper.assertTrue(!appearanceRecipe.matches(grid, helper.getLevel()), "Anvils reject basic-station appearances");
                String conversionId = host.prefix.isEmpty() ? "storage_tinkers_anvil_upgrade_from_backpack_tinkers_anvil_upgrade" : "backpack_tinkers_anvil_upgrade_from_storage_tinkers_anvil_upgrade";
                var conversion = helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id(conversionId));
                if (conversion.isPresent()) {
                    grid.clearContent(); grid.setItem(1, upgrade);
                    var converted = ((CastingTierRecipe)conversion.get()).assemble(grid, helper.getLevel().registryAccess());
                    helper.assertTrue(upgrade.getTag().equals(converted.getTag()) && ItemStack.matches(TinkerStationAppearance.bound(converted), skin), "Host conversion preserves full appearance and workstation state");
                }
                var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try {
                    buffer.writeItem(upgrade);
                    helper.assertTrue(ItemStack.matches(buffer.readItem(), upgrade), "Native appearance survives item network synchronization");
                } finally { buffer.release(); }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, batch = "tinker_station_reload")
    public static void recipeReloadInvalidatesOpenResultAndReportsNativeErrors(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager(); var original = new ArrayList<>(manager.getRecipes());
        var wrapper = wrapper(helper, hosts(helper).get(0), false); fill(wrapper, building(helper, "pickaxe"));
        var menu = new TinkerStationUpgradeContainer(helper.makeMockPlayer(), 0, wrapper, SophisticatedSmeltery.TINKER_STATION_CONTAINER);
        var output = menu.resultSlot();
        helper.assertTrue(!output.getItem().isEmpty(), "Open workstation initially has a result");
        try {
            manager.replaceRecipes(List.of()); TinkerStationUpgradeWrapper.invalidateRecipes();
            helper.assertTrue(output.getItem().isEmpty(), "Removed recipes clear open previews");
        } finally { manager.replaceRecipes(original); TinkerStationUpgradeWrapper.invalidateRecipes(); }
        helper.assertTrue(!output.getItem().isEmpty(), "Restoring recipes refreshes an open result");
        ItemStack tool = wrapper.result();
        for (int i = 0; i < wrapper.inventory().getSlots(); i++) wrapper.inventory().setStackInSlot(i, ItemStack.EMPTY);
        wrapper.inventory().setStackInSlot(0, tool); wrapper.setInput(0, part(TinkerToolParts.repairKit, "iron"));
        var nativeTable = baseline(helper, wrapper); var nativeResult = nativeTable.calcResult(null);
        helper.assertTrue(ItemStack.matches(nativeResult, wrapper.result()) && java.util.Objects.equals(nativeTable.getCurrentError(), wrapper.error()), "Native invalid-repair preview and error match");
        helper.assertTrue(ItemStack.matches(nativeResult, output.getItem()), "Open result reflects native validation after inputs change");
        helper.succeed();
    }
}
