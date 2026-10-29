package org.brahypno.sophisticated_smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.item.TankItem;

@PrefixGameTestTemplate(false)
public final class UpgradeGameTests {
    private static ItemStack bound() {
        return ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), new ItemStack(TinkerSmeltery.smelteryController));
    }
    private static BackpackWrapper backpack() {
        BackpackWrapper storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler(); // Initialize the normal backpack contents UUID.
        storage.getUpgradeHandler().setStackInSlot(0, bound());
        return storage;
    }
    private static MeltingUpgradeWrapper melting(BackpackWrapper storage) {
        return (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
    }
    private static void materials(MeltingUpgradeWrapper wrapper) {
        wrapper.getComponents().setStackInSlot(0, new ItemStack(TinkerSmeltery.searedBricks, 4));
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void controllerAppearanceTracksIgnitionAndHeatingSwitch(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = melting(storage);
        materials(wrapper);
        int[] rebuilds = {0};
        storage.getRenderInfo().setRenderUpdateChangeListener(info -> rebuilds[0]++);
        var burn = new CompoundTag();
        burn.putInt("fuel", 100);
        wrapper.getFurnace().fuel().readFromTag(burn);
        wrapper.persist();
        helper.assertTrue(ControllerBinding.isActive(storage.getRenderInfo().getUpgradeItems().get(0)) && rebuilds[0] == 1,
                "Ignition refreshes the equipment snapshot and requests a placed model rebuild");
        wrapper.persist();
        helper.assertTrue(rebuilds[0] == 1, "Unchanged burning state must not rebuild the model");
        wrapper.setHeating(false);
        helper.assertTrue(!ControllerBinding.isActive(storage.getRenderInfo().getUpgradeItems().get(0)) && rebuilds[0] == 2,
                "Heating off hides fire even with paid fuel remaining");
        wrapper.setHeating(true);
        helper.assertTrue(ControllerBinding.isActive(storage.getRenderInfo().getUpgradeItems().get(0)) && rebuilds[0] == 3,
                "Heating on restores the burning appearance");
        burn.putInt("fuel", 0);
        wrapper.getFurnace().fuel().readFromTag(burn);
        wrapper.persist();
        helper.assertTrue(!ControllerBinding.isActive(storage.getRenderInfo().getUpgradeItems().get(0)) && rebuilds[0] == 4,
                "Exhausted fuel refreshes the idle appearance");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, timeoutTicks = 100)
    public static void missingControllerFallsBackWithoutLosingStoredState(GameTestHelper helper) {
        var donor = melting(backpack());
        materials(donor);
        ItemStack fuelTank = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK));
        TankItem.setTank(fuelTank, new FluidStack(Fluids.LAVA, 1000));
        donor.getComponents().setStackInSlot(1, fuelTank);
        donor.getContainers().setStackInSlot(1, new ItemStack(Items.MILK_BUCKET));
        donor.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 90), IFluidHandler.FluidAction.EXECUTE);
        donor.persist();
        ItemStack saved = donor.getUpgradeStack().copy();
        saved.getTag().getCompound(ControllerBinding.CONTROLLER).putString("id",
                net.minecraftforge.fml.ModList.get().isLoaded("esotericism_tinker")
                        ? "missing_test_mod:controller" : "esotericism_tinker:transmute_controller");
        var state = saved.getTag().getCompound(ControllerBinding.STATE);
        var unknown = new CompoundTag();
        unknown.putString("id", "missing_test_mod:component");
        unknown.putByte("Count", (byte)3);
        unknown.putInt("Slot", 8);
        state.getCompound("components").getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND).add(unknown);
        var drops = new net.minecraft.nbt.ListTag();
        drops.add(new ItemStack(Items.DIAMOND, 7).save(new CompoundTag()));
        state.put("mobDrops", drops);
        CompoundTag expected = saved.getTag().copy();

        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, saved);
        var empty = melting(storage);
        helper.assertTrue(!empty.isOperational() && empty.getFurnace() == null && !empty.isHeating()
                        && empty.getMeltingSlotCount() == 0 && empty.getTank().getCapacity() == 0 && empty.canRemove(),
                "A missing controller becomes an empty inactive and removable upgrade");
        helper.assertTrue(saved.getMaxStackSize() == 1 && saved.getHoverName().equals(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()).getHoverName()),
                "Display the blank upgrade name but keep saved contents in a single item");
        helper.assertTrue(empty.getComponents().insertItem(0, new ItemStack(TinkerSmeltery.searedBricks), false).getCount() == 1
                        && empty.getContainers().insertItem(0, new ItemStack(Items.BUCKET), false).getCount() == 1,
                "Inactive slots must reject items instead of accepting unsaved contents");
        var fluid = empty.wrapHandler(null, storage.getBackpack());
        helper.assertTrue(fluid.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0
                        && fluid.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "A dormant upgrade cannot expose or change its preserved furnace fluids");
        var player = helper.makeMockPlayer();
        var container = new MeltingUpgradeContainer(player, 0, empty, SophisticatedSmeltery.MELTING_CONTAINER);
        var action = new CompoundTag();
        action.putString("action", "heating");
        container.handleMessage(action);
        empty.tick(null, helper.getLevel(), BlockPos.ZERO);
        empty.persist();
        empty.onBeforeRemoved();
        helper.assertTrue(saved.getTag().equals(expected), "Preserve the exact original NBT including missing IDs, fluids, containers and drops");

        ItemStack restoredStack = saved.copy();
        restoredStack.getTag().getCompound(ControllerBinding.CONTROLLER).putString("id", "tconstruct:smeltery_controller");
        var restored = new MeltingUpgradeWrapper(storage, restoredStack, ignored -> {});
        helper.assertTrue(restored.isOperational() && restored.getTank().getContained() == 90
                        && restored.getComponents().getStackInSlot(0).getCount() == 4
                        && restored.getComponents().getStackInSlot(1).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM)
                            .orElseThrow(IllegalStateException::new).getFluidInTank(0).getAmount() == 1000,
                "Recreating an available controller restores its saved fluid and materials");

        helper.setBlock(BlockPos.ZERO, ModBlocks.NETHERITE_BACKPACK.get());
        var block = (BackpackBlockEntity)helper.getBlockEntity(BlockPos.ZERO);
        block.setBackpack(storage.getBackpack());
        ItemStack held = storage.getBackpack().copy();
        helper.onEachTick(() -> ((BackpackItem)held.getItem()).inventoryTick(held, helper.getLevel(), player, -1, false));
        helper.succeedWhen(() -> {
            var placed = (MeltingUpgradeWrapper)block.getBackpackWrapper().getUpgradeHandler().getSlotWrappers().get(0);
            helper.assertTrue(!placed.isOperational() && placed.getUpgradeStack().getTag().equals(expected),
                    "Actual placed and carried host loading keeps the missing-controller upgrade inactive and intact");
        });
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void controllerInstallationAndNativeNames(GameTestHelper helper) {
        MeltingUpgradeItem item = SophisticatedSmeltery.MELTING_UPGRADE.get();
        ItemStack blank = new ItemStack(item);
        helper.assertTrue(blank.getTag() == null, "Blank upgrade must not need NBT");
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        helper.assertTrue(!item.canAddUpgradeTo(storage, blank, true, false).isSuccessful(), "Blank upgrade cannot be installed");
        helper.assertTrue(item.canAddUpgradeTo(storage, bound(), true, false).isSuccessful(), "Bound upgrade can be installed");
        storage.getUpgradeHandler().setStackInSlot(0, bound());
        helper.assertTrue(!item.canAddUpgradeTo(storage, bound(), true, false).isSuccessful(), "Controller must be unique");
        ItemStack controller = new ItemStack(TinkerSmeltery.foundryController);
        Component expected = ControllerBinding.create(controller).getDisplayName();
        ItemStack bound = ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), controller);
        var contents = (net.minecraft.network.chat.contents.TranslatableContents)bound.getHoverName().getContents();
        helper.assertTrue(contents.getArgs().length == 1 && expected.equals(contents.getArgs()[0]), "Upgrade suffix must contain the actual native controller name component");
        helper.assertTrue(expected.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents, "Keep the original translation key for language and resource-pack changes");
        controller.setHoverName(Component.literal("Named foundry"));
        bound = ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), controller);
        contents = (net.minecraft.network.chat.contents.TranslatableContents)bound.getHoverName().getContents();
        helper.assertTrue(controller.getHoverName().equals(contents.getArgs()[0]), "Retain a custom-named controller's name");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void capacityAndFuelGuardMaterialAndUpgradeRemoval(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = melting(storage);
        materials(wrapper);
        int capacity = wrapper.getTank().getCapacity();
        helper.assertTrue(capacity > 0, "Four bricks plus controller must provide minimum structure");
        wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), capacity), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(!SophisticatedSmeltery.MELTING_UPGRADE.get().canRemoveUpgradeFrom(storage, false).isSuccessful(), "Filled controller cannot be removed");
        helper.assertTrue(wrapper.getComponents().extractItem(0, 4, false).isEmpty(), "Cannot remove materials needed by stored fluid");
        helper.assertTrue(wrapper.getTank().getContained() == capacity && wrapper.getTank().getCapacity() == capacity, "Simulated removal must not change capacity or contents");
        wrapper.getTank().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(wrapper.getComponents().extractItem(0, 4, false).getCount() == 4, "Empty furnace allows material removal");
        helper.assertTrue(wrapper.getTank().getCapacity() == 0, "No materials means no capacity");

        materials(wrapper);
        ItemStack fuelTank = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK));
        TankItem.setTank(fuelTank, new FluidStack(Fluids.LAVA, 1000));
        wrapper.getComponents().setStackInSlot(1, fuelTank);
        helper.assertTrue(!wrapper.canRemove(), "Internal lava locks controller removal");
        helper.assertTrue(ControllerBinding.cleanForBinding(wrapper.getUpgradeStack()), "Rebinding refunds the fuel tank");
        helper.assertTrue(wrapper.getFurnace().fuel().getFuelInfo().getTemperature() > 0, "Display available fuel temperature");
        wrapper.getComponents().extractItem(1, 1, false);
        helper.assertTrue(wrapper.getFurnace().fuel().getFuelInfo().isEmpty(), "Removing tank clears stale fuel display");
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
        wrapper = melting(storage);
        wrapper.tick(null, helper.getLevel(), BlockPos.ZERO);
        capacity = wrapper.getTank().getCapacity();
        helper.assertTrue(capacity > wrapper.getFurnace().baseCapacity(), "Use native tank stack scaling");
        wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), capacity), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(!ModItems.STACK_UPGRADE_TIER_2.get().canRemoveUpgradeFrom(storage, false).isSuccessful(), "Reject overflowing stack upgrade removal");
        helper.assertTrue(!ModItems.STACK_UPGRADE_TIER_2.get().canSwapUpgradeFor(new ItemStack(ModItems.STACK_UPGRADE_STARTER_TIER.get()), 1, storage, false).isSuccessful(),
                "Reject overflowing stack upgrade downgrade");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, timeoutTicks = 650)
    public static void realBackpackSlotsSurviveSortingAndNormalInsertion(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = melting(storage);
        materials(wrapper);
        helper.assertTrue(wrapper.getMeltingSlotCount() == 1 && wrapper.isMeltingSlot(0) && !wrapper.isMeltingSlot(1),
                "Only first X real backpack slots participate");
        wrapper.setHeating(false);
        storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        storage.getInventoryHandler().setStackInSlot(5, new ItemStack(Items.IRON_INGOT, 64));
        wrapper.tick(null, helper.getLevel(), BlockPos.ZERO);
        helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).is(Items.GOLD_INGOT), "Binding does not relocate input");
        wrapper.getFurnace().melting().heatItems(3000, 5);
        helper.assertTrue(wrapper.getFurnace().melting().getCurrentTime(0) > 0, "Start real gold recipe progress before sorting");
        storage.setSortBy(net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy.COUNT);
        storage.sort();
        helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).is(Items.IRON_INGOT)
                        && storage.getInventoryHandler().getStackInSlot(1).is(Items.GOLD_INGOT),
                "Native sort must include the melting slot and move the actual stacks");
        wrapper.getFurnace().refreshInputs();
        helper.assertTrue(wrapper.getFurnace().melting().getCurrentTime(0) == 0, "Moved gold progress must not apply to the sorted iron input");
        wrapper.getFurnace().melting().heatItems(3000, 100000);
        wrapper.getFurnace().melting().heatItems(3000, 100000);
        helper.assertTrue(wrapper.getTank().getContained() == 90 && wrapper.getTank().getFluidInTank(0).getFluid() == TinkerFluids.moltenIron.get(),
                "The newly sorted real slot must use its own iron recipe");
        helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).getCount() == 63
                        && storage.getInventoryHandler().getStackInSlot(1).getCount() == 1,
                "Exactly one sorted iron input is consumed and the moved gold is retained");
        helper.assertTrue(storage.getInventoryForInputOutput().insertItem(0, new ItemStack(Items.IRON_INGOT), false).isEmpty()
                        && storage.getInventoryHandler().getStackInSlot(0).getCount() == 64,
                "The same melting slot must remain accessible to normal backpack automation input");
        helper.succeed();
    }

    private static void prepareFuelAndInput(BackpackWrapper storage) {
        var wrapper = melting(storage);
        materials(wrapper);
        ItemStack tank = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK));
        TankItem.setTank(tank, new FluidStack(Fluids.LAVA, 1000));
        wrapper.getComponents().setStackInSlot(1, tank);
        storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, timeoutTicks = 650)
    public static void placedAndCarriedHostsMeltAndPersist(GameTestHelper helper) {
        var placed = backpack();
        var carried = backpack();
        prepareFuelAndInput(placed);
        prepareFuelAndInput(carried);
        helper.setBlock(BlockPos.ZERO, ModBlocks.NETHERITE_BACKPACK.get());
        var block = (BackpackBlockEntity)helper.getBlockEntity(BlockPos.ZERO);
        block.setBackpack(placed.getBackpack());
        ItemStack stack = carried.getBackpack();
        var player = helper.makeMockPlayer();
        helper.onEachTick(() -> ((BackpackItem)stack.getItem()).inventoryTick(stack, helper.getLevel(), player, -1, false));
        helper.succeedWhen(() -> {
            var held = stack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
            for (var storage : java.util.List.of(block.getBackpackWrapper(), held)) {
                var actual = (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
                helper.assertTrue(actual.getTank().getContained() == 90 && storage.getInventoryHandler().getStackInSlot(0).isEmpty(),
                        "Real placed and carried dispatch each consume one input and produce one ingot");
                var fuel = actual.getComponents().getStackInSlot(1).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM)
                        .orElseThrow(IllegalStateException::new);
                helper.assertTrue(fuel.getFluidInTank(0).getAmount() < 1000, "Actual host consumes installed fuel");
                var restored = new MeltingUpgradeWrapper(storage, actual.getUpgradeStack().copy(), ignored -> {});
                helper.assertTrue(restored.getTank().getContained() == 90 && restored.getFurnace().fuel().getFuel() > 0,
                        "Actual host persists output and remaining burn");
            }
        });
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void bindingAndClearingRefundEveryStoredItem(GameTestHelper helper) {
        var visible = helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id("foundry_upgrade")).orElseThrow();
        helper.assertTrue(!visible.isSpecial() && visible.getIngredients().size() == 2
                && ControllerBinding.getController(visible.getResultItem(helper.getLevel().registryAccess())).is(TinkerSmeltery.foundryController.asItem()),
                "Expose ingredients and a bound result to JEI and the recipe book");
        for (int width : new int[]{2, 3}) {
            for (boolean unbind : new boolean[]{false, true}) {
                var storage = backpack();
                var wrapper = melting(storage);
                var expected = new java.util.ArrayList<ItemStack>();
                for (int slot = 0; slot < 9; slot++) {
                    ItemStack component = new ItemStack(TinkerSmeltery.searedBricks, slot + 1);
                    component.setHoverName(Component.literal("Material " + slot));
                    if (slot == 1) {
                        component = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK));
                        TankItem.setTank(component, new FluidStack(Fluids.LAVA, 1000));
                    }
                    expected.add(component.copy());
                    wrapper.getComponents().setStackInSlot(slot, component);
                }
                wrapper.getContainers().setStackInSlot(0, new ItemStack(Items.BUCKET, 8));
                wrapper.getContainers().setStackInSlot(1, new ItemStack(Items.MILK_BUCKET));
                expected.add(new ItemStack(Items.BUCKET, 8));
                expected.add(new ItemStack(Items.MILK_BUCKET));
                ItemStack controller = ControllerBinding.getController(wrapper.getUpgradeStack());
                controller.setHoverName(Component.literal("Original furnace"));
                wrapper.getUpgradeStack().getOrCreateTag().put(ControllerBinding.CONTROLLER, controller.save(new CompoundTag()));
                var player = helper.makeMockPlayer();
                var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
                    public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
                    public boolean stillValid(Player p) { return true; }
                }, width, width);
                grid.setItem(width * width - 1, wrapper.getUpgradeStack().copy());
                if (!unbind) grid.setItem(0, new ItemStack(TinkerSmeltery.foundryController));
                var recipe = helper.getLevel().getRecipeManager().getRecipeFor(
                        net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, helper.getLevel()).orElseThrow();
                ItemStack output = recipe.assemble(grid, helper.getLevel().registryAccess());
                helper.assertTrue(!output.hasTag() || !output.getTag().contains(ControllerBinding.STATE), "Never copy refunded furnace contents into output");
                helper.assertTrue(unbind ? ControllerBinding.getController(output).isEmpty()
                                : ControllerBinding.getController(output).is(TinkerSmeltery.foundryController.asItem()),
                        "Real recipe manager selects the requested unbinding or foundry conversion");
                ItemStack independent = recipe.getRemainingItems(grid).get(width * width - 1);
                independent.getOrCreateTag().putInt("ownership", 42);
                helper.assertTrue(!controller.getTag().contains("ownership")
                        && !ControllerBinding.getController(grid.getItem(width * width - 1)).getTag().contains("ownership"),
                        "Controller remainder owns its NBT independently");
                var result = new net.minecraft.world.inventory.ResultContainer();
                result.setItem(0, output);
                result.setRecipeUsed(recipe);
                var resultSlot = new net.minecraft.world.inventory.ResultSlot(player, grid, result, 0, 0, 0);
                resultSlot.onTake(player, resultSlot.remove(1));
                helper.assertTrue(ItemStack.matches(grid.getItem(width * width - 1), controller), "Actual result-slot consumption returns the exact old furnace");
                for (ItemStack refund : expected) {
                    int count = 0;
                    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        ItemStack returned = player.getInventory().getItem(slot);
                        if (ItemStack.isSameItemSameTags(refund, returned)) count += returned.getCount();
                    }
                    helper.assertTrue(count == refund.getCount(), "Return every internal stack exactly once, including tank fluid and custom NBT: " + refund);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void clearingRecipeNetworkAndMoltenContentsGuard(GameTestHelper helper) {
        var id = new net.minecraft.resources.ResourceLocation("sophisticatedcore", "upgrade_clear");
        var loaded = helper.getLevel().getRecipeManager().byKey(id).orElseThrow();
        helper.assertTrue(loaded instanceof org.brahypno.sophisticated_smeltery.recipe.UpgradeClearingRecipe,
                "The existing clear recipe ID must load the custom implementation instead of a competing recipe");
        var recipe = (org.brahypno.sophisticated_smeltery.recipe.UpgradeClearingRecipe)loaded;
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            SophisticatedSmeltery.UPGRADE_CLEAR.get().toNetwork(buffer, recipe);
            var decoded = SophisticatedSmeltery.UPGRADE_CLEAR.get().fromNetwork(id, buffer);
            var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
                public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
                public boolean stillValid(Player p) { return true; }
            }, 2, 2);
            ItemStack otherUpgrade = new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get());
            otherUpgrade.setHoverName(Component.literal("Configured upgrade"));
            grid.setItem(2, otherUpgrade);
            helper.assertTrue(decoded.matches(grid, helper.getLevel()), "Client-decoded recipe still clears other configured upgrades");
            ItemStack cleared = decoded.assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(cleared.is(otherUpgrade.getItem()) && cleared.getCount() == 1 && !cleared.hasTag(),
                    "Preserve Core's ordinary upgrade clearing result");
            helper.assertTrue(decoded.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty) && otherUpgrade.hasTag(),
                    "Other upgrades have no controller remainder and their inputs are not mutated");
            grid.setItem(0, new ItemStack(Items.DIRT));
            helper.assertTrue(!decoded.matches(grid, helper.getLevel()), "Keep the single-input clearing rule");
            grid.setItem(0, ItemStack.EMPTY);
            grid.setItem(2, cleared);
            helper.assertTrue(!decoded.matches(grid, helper.getLevel()), "Already-clear upgrades cannot be cleared again");
        } finally { buffer.release(); }
        var storage = backpack();
        var wrapper = melting(storage);
        materials(wrapper);
        wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 90), IFluidHandler.FluidAction.EXECUTE);
        wrapper.onBeforeRemoved();
        var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(Player p) { return true; }
        }, 2, 2);
        grid.setItem(3, wrapper.getUpgradeStack().copy());
        var manager = helper.getLevel().getRecipeManager();
        helper.assertTrue(manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, helper.getLevel()).isEmpty(),
                "Core clearing cannot erase molten furnace contents");
        grid.setItem(0, new ItemStack(TinkerSmeltery.foundryController));
        helper.assertTrue(manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, helper.getLevel()).isEmpty(),
                "Drain molten contents before changing furnace type");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void bucketMessagesWaitingRetryAndPersistence(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = melting(storage);
        materials(wrapper);
        wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        var player = helper.makeMockPlayer();
        player.containerMenu.setCarried(new ItemStack(Items.BUCKET, 8));
        var container = new MeltingUpgradeContainer(player, 0, wrapper, SophisticatedSmeltery.MELTING_CONTAINER);
        CompoundTag message = new CompoundTag();
        message.putString("action", "fluid");
        message.put("fluid", new FluidStack(TinkerFluids.moltenIron.get(), 1000).writeToNBT(new CompoundTag()));
        container.handleMessage(message);
        helper.assertTrue(wrapper.getTank().getContained() == 0, "Native UI transfer must work with a stack of empty buckets");
        int filled = player.containerMenu.getCarried().is(TinkerFluids.moltenIron.get().getBucket()) ? player.containerMenu.getCarried().getCount() : 0;
        int empty = player.containerMenu.getCarried().is(Items.BUCKET) ? player.containerMenu.getCarried().getCount() : 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(TinkerFluids.moltenIron.get().getBucket())) filled += stack.getCount();
            if (stack.is(Items.BUCKET)) empty += stack.getCount();
        }
        helper.assertTrue(filled == 1 && empty == 7, "Keep exactly one filled bucket and seven empty buckets");
        wrapper.getComponents().setStackInSlot(0, new ItemStack(TinkerSmeltery.searedBricks, 32));
        wrapper.setHeating(false);
        wrapper.getContainers().setStackInSlot(0, new ItemStack(Items.BUCKET, 8));
        wrapper.tick(null, helper.getLevel(), BlockPos.ZERO);
        helper.assertTrue(wrapper.getContainers().getStackInSlot(1).isEmpty(), "Empty furnace cannot fill a waiting bucket");
        wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 2000), IFluidHandler.FluidAction.EXECUTE);
        wrapper.tick(null, helper.getLevel(), BlockPos.ZERO);
        helper.assertTrue(wrapper.getContainers().getStackInSlot(1).is(TinkerFluids.moltenIron.get().getBucket()),
                "Fluid arriving after bucket insertion must trigger normal GUI container transfer");
        helper.assertTrue(wrapper.getTank().getContained() == 1000 && wrapper.getContainers().getStackInSlot(0).getCount() == 7,
                "Consume exactly one bucket and 1000 mB, keeping the occupied output protected");
        for (int tick = 0; tick < 20; tick++) wrapper.tick(null, helper.getLevel(), BlockPos.ZERO);
        helper.assertTrue(wrapper.getTank().getContained() == 1000, "Occupied output must block further transfer");
        ItemStack first = wrapper.getContainers().extractItem(1, 1, false);
        helper.assertTrue(first.is(TinkerFluids.moltenIron.get().getBucket())
                        && wrapper.getContainers().getStackInSlot(1).is(TinkerFluids.moltenIron.get().getBucket()),
                "Taking the first bucket allows the second waiting bucket to fill");
        helper.assertTrue(wrapper.getTank().getContained() == 0 && wrapper.getContainers().getStackInSlot(0).getCount() == 6,
                "Exactly two buckets fill; remaining empty buckets stay in the input");
        var restored = new MeltingUpgradeWrapper(storage, wrapper.getUpgradeStack().copy(), ignored -> {});
        helper.assertTrue(restored.getTank().getContained() == 0 && restored.getContainers().getStackInSlot(0).getCount() == 6
                        && restored.getContainers().getStackInSlot(1).is(TinkerFluids.moltenIron.get().getBucket()),
                "Completed automatic transfers must persist all items and fluid");
        helper.succeed();
    }

}
