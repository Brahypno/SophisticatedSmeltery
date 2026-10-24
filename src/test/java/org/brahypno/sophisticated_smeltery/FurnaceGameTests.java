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
        furnace.tick(false, 1);
        helper.assertTrue(tank.getContained() == 0 && inventory.getStackInSlot(0).getCount() == 64, "Disabled cooling must not produce fluid");
        furnace.tick(true, 1);
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
        furnace.tick(false, 3);
        helper.assertTrue(furnace.fuel().getFuel() == remaining, "Disabled controller must pause remaining burn");
        furnace.tick(true, 3);
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
    public static void nativeAlloyingFollowsControllerPolicy(GameTestHelper helper) {
        NativeFurnace furnace = new NativeFurnace(new ItemStack(TinkerSmeltery.smelteryController), new CompoundTag(),
                () -> List.of(new FluidTank(1000)), () -> {});
        furnace.resize(5, 1);
        furnace.bindInputs(helper.getLevel(), new ItemStackHandler(1), new int[0]);
        helper.assertTrue(furnace.canAlloy(), "Discover the actual native smeltery alloy module");
        var tank = furnace.controller().getTank();
        tank.fill(new FluidStack(TinkerFluids.moltenCopper.get(), 90), IFluidHandler.FluidAction.EXECUTE);
        tank.fill(new FluidStack(TinkerFluids.moltenGold.get(), 90), IFluidHandler.FluidAction.EXECUTE);
        furnace.fuel().set(0, 100);
        furnace.fuel().set(2, 1000);
        furnace.tick(false, 2);
        helper.assertTrue(tank.getFluids().size() == 2, "Controller switch must prevent alloying");
        furnace.tick(true, 2);
        helper.assertTrue(tank.getFluids().size() == 1 && tank.getFluidInTank(0).getFluid() == TinkerFluids.moltenRoseGold.get(), "Use native rose gold recipe");
        helper.assertTrue(tank.getContained() == 180, "Preserve native alloy amounts");
        var foundry = new NativeFurnace(new ItemStack(TinkerSmeltery.foundryController), new CompoundTag(), List::of, () -> {});
        helper.assertTrue(!foundry.canAlloy(), "Foundry must not gain alloying");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void controllerHeatOverrideIsDispatchedWithoutWorldWrites(GameTestHelper helper) {
        NativeFurnace furnace = new NativeFurnace(new CustomHeatController(), new CompoundTag(), List::of, () -> {});
        ItemStackHandler inventory = inputs(furnace, helper);
        var originalWorldState = helper.getLevel().getBlockState(BlockPos.ZERO);
        furnace.tick(false, 1);
        helper.assertTrue(furnace.controller().getTank().getContained() == 0, "Disabled controller must skip its heat override");
        furnace.tick(true, 1);
        furnace.tick(true, 1);
        furnace.tick(true, 3);
        helper.assertTrue(furnace.controller().getTank().getContained() == 90, "Run the actual controller's heat override, not a generic fuel-only loop");
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 63, "Native override still consumes real backpack input once");
        helper.assertTrue(helper.getLevel().getBlockState(BlockPos.ZERO).equals(originalWorldState), "Detached controller cannot change a world block");
        helper.succeed();
    }

    public static final class CustomHeatController extends SmelteryBlockEntity {
        public CustomHeatController() { super(BlockPos.ZERO, TinkerSmeltery.smelteryController.get().defaultBlockState()); }
        @Override protected void heat() {
            if (tick % 4 == 1) meltingInventory.heatItems(3000, 100000);
            if (tick % 4 == 3) throw new IllegalStateException("Portable engine must not dispatch native world-update phase");
        }
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void idleNativeHeatingDoesNotRewriteUpgradeState(GameTestHelper helper) {
        int[] saves = {0};
        var furnace = new NativeFurnace(new ItemStack(TinkerSmeltery.smelteryController), new CompoundTag(),
                () -> List.of(new FluidTank(1000)), () -> saves[0]++);
        furnace.resize(5, 1);
        furnace.bindInputs(helper.getLevel(), new ItemStackHandler(1), new int[]{0});
        saves[0] = 0;
        for (int tick = 0; tick < 32; tick++) furnace.tick(true, tick);
        helper.assertTrue(saves[0] == 0, "An idle native controller must not request periodic upgrade NBT saves");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery", batch = "fuel_selection")
    public static void chooseLowestFuelMeetingReachableRecipeTemperatures(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        var recipes = new ArrayList<>(original);
        recipes.removeIf(recipe -> recipe.getType() == slimeknights.tconstruct.library.recipe.TinkerRecipeTypes.MELTING.get());
        String[] items = {"stick", "diamond", "nether_star"};
        int[] temperatures = {800, 1300, 2000};
        for (int index = 0; index < items.length; index++) {
            var json = com.google.gson.JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:" + items[index]
                    + "\"},\"result\":{\"fluid\":\"tconstruct:molten_iron\",\"amount\":90},\"temperature\":" + temperatures[index] + ",\"time\":1}").getAsJsonObject();
            recipes.add(TinkerSmeltery.meltingSerializer.get().fromJson(SophisticatedSmeltery.id("temperature_" + index), json));
        }
        try {
            manager.replaceRecipes(recipes);
            NativeFurnace.invalidateRecipes();
            for (int scenario = 0; scenario < 3; scenario++) {
                FluidTank lava = new FluidTank(1000);
                FluidTank blood = new FluidTank(1000);
                lava.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
                blood.fill(new FluidStack(TinkerFluids.blazingBlood.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
                var furnace = new NativeFurnace(new ItemStack(TinkerSmeltery.smelteryController), new CompoundTag(), () -> List.of(lava, blood), () -> {});
                furnace.resize(9, 1);
                var inventory = new ItemStackHandler(2);
                inventory.setStackInSlot(0, new ItemStack(Items.STICK));
                inventory.setStackInSlot(1, new ItemStack(scenario == 0 ? Items.DIAMOND : Items.NETHER_STAR));
                if (scenario == 2) inventory.setStackInSlot(1, ItemStack.EMPTY);
                furnace.bindInputs(helper.getLevel(), inventory, new int[]{0, 1});
                helper.assertTrue(furnace.melting().getRequiredTemp(0) == 800, "Use actual loaded low-temperature recipe");
                for (int tick = 0; tick < 40; tick++) { furnace.refreshInputs(); furnace.tick(true, tick); }
                helper.assertTrue(inventory.getStackInSlot(0).isEmpty(), "Always process the reachable low-temperature input");
                if (scenario == 0) {
                    helper.assertTrue(inventory.getStackInSlot(1).isEmpty(), "1300-degree input must melt despite lava appearing first");
                    helper.assertTrue(lava.getFluidAmount() == 1000 && blood.getFluidAmount() < 1000, "Mixed 800/1300-degree work selects blazing blood");
                    helper.assertTrue(furnace.controller().getTank().getContained() == 180, "Both real inputs produce once");
                } else {
                    helper.assertTrue(lava.getFluidAmount() < 1000 && blood.getFluidAmount() == 1000, "Low work must not waste hotter fuel, even beside unreachable 2000-degree work");
                    helper.assertTrue(furnace.controller().getTank().getContained() == 90, "Unreachable recipe must not produce or block reachable work");
                }
            }
        } finally { manager.replaceRecipes(original); NativeFurnace.invalidateRecipes(); }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery", batch = "recipe_reload")
    public static void switchingTemperaturePreservesPaidBurnAcrossReload(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        var recipes = new ArrayList<>(original);
        for (int index = 0; index < 2; index++) {
            String item = index == 0 ? "stick" : "diamond";
            int temperature = index == 0 ? 800 : 1300;
            var json = com.google.gson.JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:" + item
                    + "\"},\"result\":{\"fluid\":\"tconstruct:molten_iron\",\"amount\":90},\"temperature\":" + temperature + ",\"time\":1000}").getAsJsonObject();
            recipes.add(TinkerSmeltery.meltingSerializer.get().fromJson(SophisticatedSmeltery.id("switch_" + index), json));
        }
        try {
            manager.replaceRecipes(recipes);
            NativeFurnace.invalidateRecipes();
            FluidTank lava = new FluidTank(1000);
            FluidTank blood = new FluidTank(1000);
            lava.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
            blood.fill(new FluidStack(TinkerFluids.blazingBlood.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            var furnace = new NativeFurnace(new ItemStack(TinkerSmeltery.smelteryController), new CompoundTag(), () -> List.of(lava, blood), () -> {});
            furnace.resize(9, 1);
            var inventory = new ItemStackHandler(1);
            inventory.setStackInSlot(0, new ItemStack(Items.STICK));
            furnace.bindInputs(helper.getLevel(), inventory, new int[]{0});
            furnace.tick(true, 0);
            furnace.tick(true, 1);
            furnace.tick(true, 3);
            int lowRemaining = furnace.fuel().getFuel();
            int lowQuality = furnace.fuel().getFuelQuality();
            int lowRate = furnace.fuel().getRate();
            int lowFluid = lava.getFluidAmount();
            helper.assertTrue(lowRemaining > 0 && lowFluid < 1000, "Light and partially consume a real native lava batch");

            inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND));
            furnace.refreshInputs();
            furnace.tick(true, 4);
            helper.assertTrue(furnace.fuel().getTemperature() >= 1300, "Switch to hot fuel at the very next native ignition phase");
            furnace.tick(true, 5);
            helper.assertTrue(furnace.melting().getCurrentTime(0) > 0, "The real high-temperature recipe starts before the old batch burns out");
            furnace.tick(true, 7);
            int hotRemaining = furnace.fuel().getFuel();
            int hotTemperature = furnace.fuel().getTemperature();
            int hotRate = furnace.fuel().getRate();
            int hotFluid = blood.getFluidAmount();
            helper.assertTrue(hotFluid < 1000, "Actually pay for the newly lit hot batch");
            CompoundTag state = furnace.save();
            helper.assertTrue(state.getCompound("fuel").getList("pausedBurns", net.minecraft.nbt.Tag.TAG_COMPOUND)
                    .getCompound(0).getInt("fuel") == lowRemaining, "Preserve every remaining low-temperature burn tick");
            var upgrade = ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), new ItemStack(TinkerSmeltery.smelteryController));
            CompoundTag cachedOnly = new CompoundTag();
            cachedOnly.put("fuel", state.getCompound("fuel").copy());
            cachedOnly.getCompound("fuel").putInt("fuel", 0);
            upgrade.getOrCreateTag().put(ControllerBinding.STATE, cachedOnly);
            helper.assertTrue(!ControllerBinding.cleanForBinding(upgrade), "Paused paid fuel must prevent clean rebinding even without active burn");

            var loaded = new NativeFurnace(new ItemStack(TinkerSmeltery.smelteryController), state, () -> List.of(lava, blood), () -> {});
            loaded.resize(state.getInt("materials"), 1);
            loaded.bindInputs(helper.getLevel(), inventory, new int[]{0});
            helper.assertTrue(loaded.fuel().getFuel() == hotRemaining && loaded.fuel().getTemperature() == hotTemperature,
                    "Restore active hot fuel exactly after NBT reload");
            lava.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            blood.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            inventory.setStackInSlot(0, new ItemStack(Items.STICK));
            loaded.refreshInputs();
            loaded.tick(false, 8);
            loaded.tick(false, 11);
            helper.assertTrue(loaded.fuel().getFuel() == hotRemaining, "Disabled heating neither switches nor consumes active or cached fuel");
            loaded.tick(true, 12);
            helper.assertTrue(loaded.fuel().getFuel() == lowRemaining && loaded.fuel().getTemperature() == 1000
                    && loaded.fuel().getRate() == lowRate && loaded.fuel().getFuelQuality() == lowQuality,
                    "Resume exact paid low-temperature budget and native speed without any liquid tanks");
            inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND));
            loaded.refreshInputs();
            loaded.tick(true, 16);
            helper.assertTrue(loaded.fuel().getFuel() == hotRemaining && loaded.fuel().getTemperature() == hotTemperature
                    && loaded.fuel().getRate() == hotRate, "Switch back to the exact hot budget without buying another batch");
            helper.assertTrue(lava.isEmpty() && blood.isEmpty(), "Resuming paid burn must not create or refill fluid");
            helper.assertTrue(loaded.controller().getTank().getContained() == 0 && inventory.getStackInSlot(0).getCount() == 1,
                    "Partial work must not consume the actual input or produce premature output");
        } finally { manager.replaceRecipes(original); NativeFurnace.invalidateRecipes(); }
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
