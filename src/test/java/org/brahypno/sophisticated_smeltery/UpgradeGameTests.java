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
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.recipe.ControllerBindingRecipe;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.item.TankItem;

@GameTestHolder("sophisticatedsmeltery")
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

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void blankAndUniqueControllerRules(GameTestHelper helper) {
        MeltingUpgradeItem item = SophisticatedSmeltery.MELTING_UPGRADE.get();
        ItemStack blank = new ItemStack(item);
        helper.assertTrue(blank.getTag() == null, "Blank upgrade must not need NBT");
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        helper.assertTrue(!item.canAddUpgradeTo(storage, blank, true, false).isSuccessful(), "Blank upgrade cannot be installed");
        helper.assertTrue(item.canAddUpgradeTo(storage, bound(), true, false).isSuccessful(), "Bound upgrade can be installed");
        storage.getUpgradeHandler().setStackInSlot(0, bound());
        helper.assertTrue(!item.canAddUpgradeTo(storage, bound(), true, false).isSuccessful(), "Controller must be unique");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void boundNameUsesNativeControllerTranslationComponent(GameTestHelper helper) {
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

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void rebindingReturnsExactOriginalController(GameTestHelper helper) {
        ItemStack controller = new ItemStack(TinkerSmeltery.smelteryController);
        controller.setHoverName(Component.literal("Keep this controller"));
        controller.getOrCreateTag().putInt("testMarker", 1729);
        var menu = new AbstractContainerMenu(null, -1) {
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 2, 2);
        grid.setItem(3, ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), controller));
        grid.setItem(0, new ItemStack(TinkerSmeltery.foundryController));
        var recipe = new ControllerBindingRecipe(SophisticatedSmeltery.id("test_binding"), CraftingBookCategory.MISC);
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Binding recipe must be unordered");
        ItemStack output = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(ControllerBinding.getController(output).is(TinkerSmeltery.foundryController.asItem()), "Bind new controller");
        helper.assertTrue(ItemStack.matches(controller, recipe.getRemainingItems(grid).get(3)), "Return original controller including its NBT");
        output.getOrCreateTag().getCompound("controller").putInt("testMarker", 42);
        helper.assertTrue(controller.getTag().getInt("testMarker") == 1729, "Returned NBT must not share mutable ownership");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void controllerAndMaterialsCannotLoseStoredFluid(GameTestHelper helper) {
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
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void internalTinkersFuelTankBlocksControllerRemoval(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = melting(storage);
        materials(wrapper);
        ItemStack fuelTank = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK));
        TankItem.setTank(fuelTank, new FluidStack(Fluids.LAVA, 1000));
        wrapper.getComponents().setStackInSlot(1, fuelTank);
        helper.assertTrue(!wrapper.canRemove(), "Internal lava must be emptied before controller removal");
        helper.assertTrue(!ControllerBinding.cleanForBinding(wrapper.getUpgradeStack()), "Installed materials and fuel cannot be copied through crafting");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void stackUpgradeRemovalAndDowngradePreserveCapacity(GameTestHelper helper) {
        var storage = backpack();
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
        var wrapper = melting(storage);
        materials(wrapper);
        int capacity = wrapper.getTank().getCapacity();
        helper.assertTrue(capacity > wrapper.getFurnace().baseCapacity(), "Use native tank stack scaling");
        wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), capacity), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(!ModItems.STACK_UPGRADE_TIER_2.get().canRemoveUpgradeFrom(storage, false).isSuccessful(), "Reject stack upgrade removal when furnace would overflow");
        helper.assertTrue(!ModItems.STACK_UPGRADE_TIER_2.get().canSwapUpgradeFor(new ItemStack(ModItems.STACK_UPGRADE_STARTER_TIER.get()), 1, storage, false).isSuccessful(),
                "Reject stack upgrade downgrade when furnace would overflow");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void firstSlotsRemainRealBackpackSlots(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = melting(storage);
        materials(wrapper);
        helper.assertTrue(wrapper.getMeltingSlotCount() == 1 && wrapper.isMeltingSlot(0) && !wrapper.isMeltingSlot(1), "Use the first X available backpack slots");
        storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        wrapper.setHeating(false);
        wrapper.tick(null, helper.getLevel(), BlockPos.ZERO);
        helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).getCount() == 64, "Binding must not move backpack input");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery", timeoutTicks = 650)
    public static void nativeBackpackSortResetsMovedInputAndKeepsNormalInsertion(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = melting(storage);
        materials(wrapper);
        wrapper.setHeating(false);
        storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        storage.getInventoryHandler().setStackInSlot(5, new ItemStack(Items.IRON_INGOT, 64));
        wrapper.tick(null, helper.getLevel(), BlockPos.ZERO);
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

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery", timeoutTicks = 650)
    public static void backpackTickMeltsAndPersistsUsingInternalFuel(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = melting(storage);
        materials(wrapper);
        ItemStack fuelTank = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK));
        TankItem.setTank(fuelTank, new FluidStack(Fluids.LAVA, 1000));
        wrapper.getComponents().setStackInSlot(1, fuelTank);
        storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        helper.onEachTick(() -> wrapper.tick(null, helper.getLevel(), BlockPos.ZERO));
        helper.succeedWhen(() -> {
            helper.assertTrue(wrapper.getTank().getContained() == 90, "Real backpack ticks must produce exactly one ingot of fluid");
            helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).isEmpty(), "Consume the real backpack input");
            var fuel = wrapper.getComponents().getStackInSlot(1).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new);
            helper.assertTrue(fuel.getFluidInTank(0).getAmount() < 1000, "Drain the installed Tinkers fuel tank");
            var restored = new MeltingUpgradeWrapper(storage, wrapper.getUpgradeStack().copy(), ignored -> {});
            helper.assertTrue(restored.getTank().getContained() == 90, "Actual wrapper save must persist output");
            helper.assertTrue(restored.getFurnace().fuel().getFuel() > 0, "Persist remaining burning fuel");
        });
    }

    private static void prepareFuelAndInput(BackpackWrapper storage) {
        var wrapper = melting(storage);
        materials(wrapper);
        ItemStack tank = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK));
        TankItem.setTank(tank, new FluidStack(Fluids.LAVA, 1000));
        wrapper.getComponents().setStackInSlot(1, tank);
        storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery", timeoutTicks = 650)
    public static void placedBackpackDispatchesMeltingWithoutDirectUpgradeTick(GameTestHelper helper) {
        var storage = backpack();
        prepareFuelAndInput(storage);
        helper.setBlock(BlockPos.ZERO, ModBlocks.NETHERITE_BACKPACK.get());
        var blockEntity = (BackpackBlockEntity)helper.getBlockEntity(BlockPos.ZERO);
        blockEntity.setBackpack(storage.getBackpack());
        // The actual world block ticker must discover and run our upgrade.
        helper.succeedWhen(() -> {
            var installed = blockEntity.getBackpackWrapper();
            var actual = (MeltingUpgradeWrapper)installed.getUpgradeHandler().getSlotWrappers().get(0);
            helper.assertTrue(actual.getTank().getContained() == 90, "Placed backpack must dispatch melting through its real world ticker");
            helper.assertTrue(installed.getInventoryHandler().getStackInSlot(0).isEmpty(), "Placed backpack must consume its actual input");
        });
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery", timeoutTicks = 650)
    public static void carriedBackpackDispatchesThroughItemInventoryTick(GameTestHelper helper) {
        var storage = backpack();
        prepareFuelAndInput(storage);
        ItemStack stack = storage.getBackpack();
        var player = helper.makeMockPlayer();
        // Use the real backpack item entry point and its capability-owned wrapper.
        // Slot -1 is the backpack's native equipped-slot convention.
        helper.onEachTick(() -> ((BackpackItem)stack.getItem()).inventoryTick(stack, helper.getLevel(), player, -1, false));
        helper.succeedWhen(() -> {
            var actualStorage = stack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
            var actual = (MeltingUpgradeWrapper)actualStorage.getUpgradeHandler().getSlotWrappers().get(0);
            helper.assertTrue(actual.getTank().getContained() == 90, "Backpack item must discover and tick the installed melting upgrade");
            helper.assertTrue(actualStorage.getInventoryHandler().getStackInSlot(0).isEmpty(), "Item capability must expose the consumed input");
        });
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void visibleBindingRecipesKeepBookIngredientsAndControllerRemainders(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id("foundry_upgrade")).orElseThrow();
        helper.assertTrue(!recipe.isSpecial() && recipe.getIngredients().size() == 2, "Expose a normal crafting recipe to JEI and recipe book");
        helper.assertTrue(ControllerBinding.getController(recipe.getResultItem(helper.getLevel().registryAccess())).is(TinkerSmeltery.foundryController.asItem()), "Display a bound result instead of an empty custom-recipe result");
        var menu = new AbstractContainerMenu(null, -1) {
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 2, 2);
        var oldController = new ItemStack(TinkerSmeltery.smelteryController);
        oldController.getOrCreateTag().putInt("keep", 1300);
        grid.setItem(3, ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), oldController));
        grid.setItem(0, new ItemStack(TinkerSmeltery.foundryController));
        var crafting = (net.minecraft.world.item.crafting.CraftingRecipe)recipe;
        helper.assertTrue(crafting.matches(grid, helper.getLevel()), "Visible recipe remains unordered and supports clean rebinding");
        helper.assertTrue(ItemStack.matches(crafting.getRemainingItems(grid).get(3), oldController), "Visible recipe cannot bypass exact controller return");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void stackedBucketsInteractThroughRealContainerMessage(GameTestHelper helper) {
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
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void waitingBucketRetriesWhenFluidArrivesAndOutputIsTaken(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = melting(storage);
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

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void bucketGuiSlotsPersistAndFuelDisplayRefreshes(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = melting(storage);
        materials(wrapper);
        wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        wrapper.getContainers().setStackInSlot(0, new ItemStack(Items.BUCKET, 8));
        helper.assertTrue(wrapper.getContainers().getStackInSlot(0).getCount() == 7, "GUI input consumes exactly one bucket");
        helper.assertTrue(wrapper.getContainers().getStackInSlot(1).is(TinkerFluids.moltenIron.get().getBucket()), "GUI output receives molten iron bucket");
        helper.assertTrue(wrapper.getTank().getContained() == 0, "GUI slot transfer drains fluid once");
        var restored = new MeltingUpgradeWrapper(storage, wrapper.getUpgradeStack().copy(), ignored -> {});
        helper.assertTrue(restored.getContainers().getStackInSlot(0).getCount() == 7 && !restored.getContainers().getStackInSlot(1).isEmpty(), "Closing/reloading cannot lose bucket slots");
        ItemStack tank = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK));
        TankItem.setTank(tank, new FluidStack(Fluids.LAVA, 1000));
        wrapper.getComponents().setStackInSlot(1, tank);
        helper.assertTrue(wrapper.getFurnace().fuel().getFuelInfo().getTemperature() > 0, "Display current available fuel temperature");
        wrapper.getComponents().extractItem(1, 1, false);
        helper.assertTrue(wrapper.getFurnace().fuel().getFuelInfo().isEmpty(), "Removing a tank must immediately clear the displayed fuel and temperature");
        helper.succeed();
    }
}
