package org.brahypno.sophisticated_smeltery;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.furnace.NativeFurnace;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModule;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock;

@GameTestHolder("sophisticatedsmeltery")
@PrefixGameTestTemplate(false)
public final class FurnaceGameTests {
    private static NativeFurnace smeltery() {
        return new NativeFurnace(new ItemStack(TinkerSmeltery.smelteryController), new CompoundTag(), List::of, () -> {});
    }

    private static ItemStackHandler inputs(NativeFurnace furnace, GameTestHelper helper) {
        ItemStackHandler inventory = new ItemStackHandler(3);
        inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        furnace.resize(5, 1);
        furnace.bindInputs(helper.getLevel(), inventory, new int[]{0});
        return inventory;
    }

    private static void finish(NativeFurnace furnace) {
        furnace.refreshInputs();
        furnace.melting().heatItems(3000, 100000);
        furnace.melting().heatItems(3000, 100000);
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void minimumStructureWithoutWorldVolumeAllocation(GameTestHelper helper) {
        NativeFurnace furnace = smeltery();
        furnace.resize(4, 1);
        helper.assertTrue(furnace.baseCapacity() == 0, "Four total blocks cannot form a smeltery");
        furnace.resize(5, 1);
        helper.assertTrue(furnace.baseCapacity() == FluidValues.INGOT * 12, "Preserve native minimum capacity");
        furnace.resize(500, 1);
        helper.assertTrue(furnace.melting().getSlots() == 0, "Sizing must not allocate a module per world cell");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void inputStaysInBackpackUntilCompletion(GameTestHelper helper) {
        NativeFurnace furnace = smeltery();
        ItemStackHandler inventory = inputs(furnace, helper);
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 64, "Binding must not move input");
        furnace.melting().heatItems(3000, 100000);
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 64, "Heating must not reserve input");
        furnace.melting().heatItems(3000, 100000);
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 63, "Consume exactly one item after native output succeeds");
        helper.assertTrue(furnace.controller().getTank().getContained() == FluidValues.INGOT, "Preserve native ingot output");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void disabledControllerDoesNotRetryBlockedOutput(GameTestHelper helper) {
        NativeFurnace furnace = smeltery();
        ItemStackHandler inventory = inputs(furnace, helper);
        var tank = furnace.controller().getTank();
        tank.fill(new FluidStack(TinkerFluids.moltenIron.get(), tank.getCapacity()), IFluidHandler.FluidAction.EXECUTE);
        finish(furnace);
        helper.assertTrue(furnace.melting().getCurrentTime(0) == MeltingModule.NO_SPACE, "Full tank must block completion");
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 64, "Blocked input must remain in backpack");
        tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        furnace.tick(false, false, 1);
        helper.assertTrue(tank.getContained() == 0 && inventory.getStackInSlot(0).getCount() == 64, "Disabled cooling must not produce fluid");
        furnace.tick(true, false, 1);
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 63, "Re-enabled native cooling can complete finished work");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void replacementInvalidatesProgress(GameTestHelper helper) {
        NativeFurnace furnace = smeltery();
        ItemStackHandler inventory = inputs(furnace, helper);
        furnace.melting().heatItems(3000, 5);
        inventory.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 64));
        furnace.refreshInputs();
        helper.assertTrue(furnace.melting().getCurrentTime(0) == 0, "Replacement cannot inherit progress");
        finish(furnace);
        helper.assertTrue(furnace.controller().getTank().getFluidInTank(0).getFluid() == TinkerFluids.moltenGold.get(), "Only the replacement may melt");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void reloadPreservesProgressWithoutCreatingItems(GameTestHelper helper) {
        NativeFurnace original = smeltery();
        ItemStackHandler inventory = inputs(original, helper);
        original.melting().heatItems(3000, 5);
        int progress = original.melting().getCurrentTime(0);
        NativeFurnace restored = new NativeFurnace(new ItemStack(TinkerSmeltery.smelteryController), original.save(), List::of, () -> {});
        restored.resize(5, 1);
        restored.bindInputs(helper.getLevel(), inventory, new int[]{0});
        helper.assertTrue(restored.melting().getCurrentTime(0) == progress, "Progress must survive reload");
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 64, "Reload cannot create or move input");
        finish(restored);
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 63, "Restored task may complete only once");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void disabledControllerPausesBurningFuel(GameTestHelper helper) {
        FluidTank fuelTank = new FluidTank(1000);
        fuelTank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
        NativeFurnace furnace = new NativeFurnace(new ItemStack(TinkerSmeltery.smelteryController), new CompoundTag(), () -> List.of(fuelTank), () -> {});
        inputs(furnace, helper);
        furnace.fuel().findFuel(true);
        int remaining = furnace.fuel().getFuel();
        helper.assertTrue(remaining > 0, "Recognize lava through native fuel recipes");
        furnace.tick(false, false, 3);
        helper.assertTrue(furnace.fuel().getFuel() == remaining, "Disabled controller must pause remaining burn");
        furnace.tick(true, false, 3);
        helper.assertTrue(furnace.fuel().getFuel() < remaining, "Enabled controller must consume fuel");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void newlyFilledInternalFuelTankTakesPriority(GameTestHelper helper) {
        FluidTank internal = new FluidTank(1000);
        FluidTank backpack = new FluidTank(1000);
        backpack.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
        NativeFurnace furnace = new NativeFurnace(new ItemStack(TinkerSmeltery.smelteryController), new CompoundTag(),
                () -> List.of(internal, backpack), () -> {});
        inputs(furnace, helper);
        furnace.fuel().findFuel(true);
        int backpackRemaining = backpack.getFluidAmount();
        helper.assertTrue(backpackRemaining < 1000, "Use backpack fuel when internal tank is empty");
        internal.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
        furnace.fuel().findFuel(true);
        helper.assertTrue(internal.getFluidAmount() < 1000, "Newly available internal fuel must take priority");
        helper.assertTrue(backpack.getFluidAmount() == backpackRemaining, "Do not keep preferring a cached fallback tank");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void nativeAlloyingHonorsBothSwitches(GameTestHelper helper) {
        NativeFurnace furnace = smeltery();
        furnace.resize(5, 1);
        furnace.bindInputs(helper.getLevel(), new ItemStackHandler(1), new int[0]);
        helper.assertTrue(furnace.canAlloy(), "Discover the actual native smeltery alloy module");
        var tank = furnace.controller().getTank();
        tank.fill(new FluidStack(TinkerFluids.moltenCopper.get(), 90), IFluidHandler.FluidAction.EXECUTE);
        tank.fill(new FluidStack(TinkerFluids.moltenGold.get(), 90), IFluidHandler.FluidAction.EXECUTE);
        furnace.fuel().set(0, 100);
        furnace.fuel().set(2, 1000);
        furnace.tick(true, false, 2);
        helper.assertTrue(tank.getFluids().size() == 2, "Alloy switch must prevent alloying");
        furnace.tick(false, true, 2);
        helper.assertTrue(tank.getFluids().size() == 2, "Controller switch must prevent alloying");
        furnace.tick(true, true, 2);
        helper.assertTrue(tank.getFluids().size() == 1 && tank.getFluidInTank(0).getFluid() == TinkerFluids.moltenRoseGold.get(), "Use native rose gold recipe");
        helper.assertTrue(tank.getContained() == 180, "Preserve native alloy amounts");
        var foundry = new NativeFurnace(new ItemStack(TinkerSmeltery.foundryController), new CompoundTag(), List::of, () -> {});
        helper.assertTrue(!foundry.canAlloy(), "Foundry must not gain alloying");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery", batch = "recipe_reload")
    public static void removedRecipeCannotFinishFromCache(GameTestHelper helper) {
        NativeFurnace furnace = smeltery();
        ItemStackHandler inventory = inputs(furnace, helper);
        furnace.melting().heatItems(3000, 5);
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        try {
            manager.replaceRecipes(List.of());
            NativeFurnace.invalidateRecipes();
            furnace.refreshRecipes();
            finish(furnace);
            helper.assertTrue(furnace.controller().getTank().getContained() == 0, "Removed recipe cannot output from a stale module cache");
            helper.assertTrue(inventory.getStackInSlot(0).getCount() == 64, "Recipe removal must not consume input");
        } finally {
            manager.replaceRecipes(original);
            NativeFurnace.invalidateRecipes();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void foundryMatchesNativeOreAndByproducts(GameTestHelper helper) {
        ItemStack controllerStack = new ItemStack(TinkerSmeltery.foundryController);
        NativeFurnace portable = new NativeFurnace(controllerStack, new CompoundTag(), List::of, () -> {});
        portable.resize(17, 1);
        helper.assertTrue(portable.baseCapacity() == FluidValues.INGOT * 8 * 18, "Preserve native foundry capacity");
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, new ItemStack(Items.RAW_IRON, 64));
        portable.bindInputs(helper.getLevel(), inventory, new int[]{0});
        finish(portable);
        var nativeController = ControllerBinding.create(controllerStack);
        nativeController.setLevel(helper.getLevel());
        nativeController.getTank().setCapacity(portable.baseCapacity());
        nativeController.getMeltingInventory().resize(1, ignored -> {});
        nativeController.getMeltingInventory().setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        nativeController.getMeltingInventory().heatItems(3000, 100000);
        nativeController.getMeltingInventory().heatItems(3000, 100000);
        helper.assertTrue(portable.controller().getTank().getFluids().equals(nativeController.getTank().getFluids()), "Foundry outputs must match native outputs");
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 63, "Foundry consumes only one input");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void actualSubclassOutputAndCapacityAreUsed(GameTestHelper helper) {
        NativeFurnace furnace = new NativeFurnace(new CustomController(), new CompoundTag(), List::of, () -> {});
        ItemStackHandler inventory = inputs(furnace, helper);
        helper.assertTrue(furnace.baseCapacity() == FluidValues.INGOT * 24, "Invoke actual capacity override");
        finish(furnace);
        helper.assertTrue(furnace.controller().getTank().getFluidInTank(0).getFluid() == TinkerFluids.moltenGold.get(), "Invoke custom tryFillTank");
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 63, "Consume custom module input from backpack");
        helper.succeed();
    }

    public static final class CustomController extends SmelteryBlockEntity {
        public CustomController() { super(BlockPos.ZERO, TinkerSmeltery.smelteryController.get().defaultBlockState()); }
        @Override protected MeltingModuleInventory createMeltingInventory() {
            return new MeltingModuleInventory(this, tank, Config.COMMON.smelteryOreRate) {
                @Override protected boolean tryFillTank(int index, IMeltingRecipe recipe) {
                    FluidStack output = new FluidStack(TinkerFluids.moltenGold.get(), FluidValues.INGOT);
                    if (fluidHandler.fill(output, IFluidHandler.FluidAction.SIMULATE) != output.getAmount()) return false;
                    fluidHandler.fill(output, IFluidHandler.FluidAction.EXECUTE);
                    return true;
                }
            };
        }
        @Override protected void setStructure(HeatingStructureMultiblock.StructureData structure) {
            super.setStructure(structure);
            if (structure != null) tank.setCapacity(tank.getCapacity() * 2);
        }
    }
}
