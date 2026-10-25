package org.brahypno.sophisticated_smeltery;

import java.util.List;
import java.util.ArrayList;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.furnace.NativeFurnace;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.fluids.TinkerFluids;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.minecraft.core.BlockPos;

@GameTestHolder(SophisticatedSmeltery.MODID)
@PrefixGameTestTemplate(false)
public final class EstinkerBaselineGameTests {
    private static ItemStack controller() {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("esotericism_tinker", "transmute_controller")));
    }

    private static void finish(MeltingModuleInventory inventory) {
        inventory.heatItems(3000, 100000);
        inventory.heatItems(3000, 100000);
    }

    private static ItemStack component(String name, int count) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("esotericism_tinker", name)), count);
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, batch = "estinker_components")
    public static void nineSlotHeatComponentsApplyReloadAndUseNativeHeating(GameTestHelper helper) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("esotericism_tinker")) { helper.succeed(); return; }
        var config = Class.forName("org.brahypno.esotericismtinker.Config");
        int heaterTemperature = config.getField("transmuteHeaterTemperature").getInt(null);
        int acceleratorRate = config.getField("transmuteAcceleratorTemperature").getInt(null);
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, ControllerBinding.bind(
                new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), controller()));
        var wrapper = (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
        wrapper.setHeating(false);
        var blocks = wrapper.getComponents();
        helper.assertTrue(blocks.getSlots() == 9 && blocks.isItemValid(2, component("ashen_heater", 1))
                && blocks.isItemValid(8, component("ashen_accelerator", 1)), "Use the existing nine structural slots");
        blocks.setStackInSlot(0, component("ashen_bricks", 64));
        blocks.setStackInSlot(1, component("ashen_fuel_tank", 1));
        blocks.setStackInSlot(2, component("ashen_heater", 2));
        blocks.setStackInSlot(8, component("ashen_accelerator", 3));
        var furnace = wrapper.getFurnace();
        helper.assertTrue(furnace.heatingTemperature() == heaterTemperature * 2, "Count each heater in a stack");
        int capacity = furnace.baseCapacity();
        blocks.setStackInSlot(2, component("ashen_accelerator", 2));
        helper.assertTrue(furnace.heatingTemperature() == 0 && furnace.baseCapacity() == capacity,
                "Replace an equal-sized stack without a resize and immediately remove its heat");
        blocks.setStackInSlot(2, component("ashen_heater", 2));
        storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        wrapper.tick(null, helper.getLevel(), BlockPos.ZERO);
        furnace.refreshInputs();
        furnace.melting().getModule(0).set(0, 0);
        furnace.tick(true, 0);
        furnace.tick(true, 1);
        int accelerated = furnace.melting().getCurrentTime(0);
        helper.assertTrue(accelerated == heaterTemperature * 2 / 100 + acceleratorRate * 3
                && !furnace.fuel().hasFuel(), "Run actual native heat using heater temperature and accelerator rate without burning liquid");
        furnace.tick(false, 1);
        helper.assertTrue(furnace.melting().getCurrentTime(0) < accelerated, "The shared heating switch still disables component heating");
        blocks.extractItem(8, 3, false);
        furnace.melting().getModule(0).set(0, 0);
        furnace.tick(true, 1);
        helper.assertTrue(furnace.melting().getCurrentTime(0) == heaterTemperature * 2 / 100,
                "Removing accelerators immediately restores the native unaccelerated rate");
        blocks.setStackInSlot(8, component("ashen_accelerator", 3));
        wrapper.persist();
        var restored = new MeltingUpgradeWrapper(storage, wrapper.getUpgradeStack().copy(), ignored -> {});
        helper.assertTrue(restored.getFurnace().heatingTemperature() == heaterTemperature * 2,
                "Reconstruct heating from the saved nine slots before a host tick");
        restored.tick(null, helper.getLevel(), BlockPos.ZERO);
        restored.getFurnace().melting().getModule(0).set(0, 0);
        restored.getFurnace().tick(true, 1);
        helper.assertTrue(restored.getFurnace().melting().getCurrentTime(0) == accelerated,
                "Restore both configured effects after reload");

        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        try {
            // One heater plus lava reaches this recipe; hotter fuel must not be selected unnecessarily.
            var json = JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:stick\"},\"result\":{\"fluid\":\"tconstruct:molten_iron\",\"amount\":90},\"temperature\":"
                    + (heaterTemperature + 800) + ",\"time\":1000}").getAsJsonObject();
            var recipes = new ArrayList<>(original);
            recipes.add(TinkerSmeltery.meltingSerializer.get().fromJson(SophisticatedSmeltery.id("heater_assisted"), json));
            manager.replaceRecipes(recipes);
            NativeFurnace.invalidateRecipes();
            blocks.setStackInSlot(2, component("ashen_heater", 1));
            var lava = new FluidTank(4000);
            lava.fill(new FluidStack(Fluids.LAVA, 4000), FluidAction.EXECUTE);
            var hot = new FluidTank(4000);
            hot.fill(new FluidStack(TinkerFluids.blazingBlood.get(), 4000), FluidAction.EXECUTE);
            var assisted = new NativeFurnace(controller(), new CompoundTag(), () -> List.of(lava, hot), () -> {});
            assisted.resize(100, 1);
            assisted.updateHeatComponents(blocks);
            var inputs = new ItemStackHandler(1);
            inputs.setStackInSlot(0, new ItemStack(Items.STICK, 2));
            assisted.bindInputs(helper.getLevel(), inputs, new int[]{0});
            assisted.tick(true, 0);
            assisted.tick(true, 1);
            helper.assertTrue(assisted.fuel().getTemperature() == 1000 && lava.getFluidAmount() < 4000
                    && hot.getFluidAmount() == 4000 && assisted.melting().getCurrentTime(0) > 0,
                    "Include heater temperature when selecting the lowest sufficient fuel");
        } finally { manager.replaceRecipes(original); NativeFurnace.invalidateRecipes(); }
        SophisticatedSmeltery.LOGGER.info("ESTINKER COMPONENTS PASSED: nine slots, stack counts, same-count replacement, native speed, removal, switch, reload and heater-assisted fuel");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void transmuteCraftingBindingAndNetworkRecipe(GameTestHelper helper) {
        if (!ModList.get().isLoaded("esotericism_tinker")) { helper.succeed(); return; }
        var level = helper.getLevel();
        var recipe = (org.brahypno.sophisticated_smeltery.recipe.VisibleControllerBindingRecipe)
                level.getRecipeManager().byKey(SophisticatedSmeltery.id("transmute_upgrade")).orElseThrow();
        helper.assertTrue(!recipe.isSpecial() && recipe.getIngredients().size() == 2
                && ItemStack.isSameItemSameTags(ControllerBinding.getController(recipe.getResultItem(level.registryAccess())), controller()),
                "Expose the Transmute ingredients and bound result to JEI and the recipe book");
        for (int width : new int[]{2, 3}) {
            ItemStack old = new ItemStack(TinkerSmeltery.smelteryController);
            old.setHoverName(Component.literal("Old named controller"));
            ItemStack next = controller();
            next.getOrCreateTag().putString("baseline", "preserve-controller-nbt");
            var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
                public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
                public boolean stillValid(Player player) { return true; }
            }, width, width);
            for (boolean rebind : new boolean[]{false, true}) {
                ItemStack upgrade = new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get());
                if (rebind) upgrade = ControllerBinding.bind(upgrade, old);
                grid.setItem(0, next.copy());
                grid.setItem(width * width - 1, upgrade);
                helper.assertTrue(recipe.matches(grid, level), "Craft or rebind in both 2x2 and 3x3 grids");
                ItemStack result = recipe.assemble(grid, level.registryAccess());
                helper.assertTrue(ItemStack.matches(ControllerBinding.getController(result), next)
                        && result.getCount() == 1 && !result.getTag().contains(ControllerBinding.STATE),
                        "Bind the exact new controller once, without transferring old furnace state");
                helper.assertTrue(ItemStack.matches(recipe.getRemainingItems(grid).get(width * width - 1), rebind ? old : ItemStack.EMPTY),
                        "Return the old controller exactly once when rebinding");
            }
        }
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var serializer = SophisticatedSmeltery.VISIBLE_BINDING.get();
            serializer.toNetwork(buffer, recipe);
            var decoded = serializer.fromNetwork(recipe.getId(), buffer);
            helper.assertTrue(ItemStack.matches(decoded.getResultItem(level.registryAccess()), recipe.getResultItem(level.registryAccess())),
                    "Preserve the JEI/recipe-book result through client recipe synchronization");
        } finally { buffer.release(); }
        SophisticatedSmeltery.LOGGER.info("ESTINKER CRAFTING: blank/rebind, 2x2/3x3, controller NBT, remainder and network passed");
        helper.succeed();
    }

    private static NativeFurnace compareNative(GameTestHelper helper, ItemStack input, int freeSpace) {
        var portable = new NativeFurnace(controller(), new CompoundTag(), List::of, () -> {});
        portable.resize(100, 1);
        var backpack = new ItemStackHandler(1);
        backpack.setStackInSlot(0, input.copyWithCount(64));
        // A one-item tail batch is tested using an actual one-item backpack stack.
        if (input.getCount() == 1) backpack.setStackInSlot(0, input.copy());
        portable.bindInputs(helper.getLevel(), backpack, new int[]{0});
        HeatingStructureBlockEntity nativeController = ControllerBinding.create(controller());
        nativeController.setLevel(helper.getLevel());
        nativeController.getTank().setCapacity(portable.baseCapacity());
        nativeController.getMeltingInventory().resize(1, ignored -> {});
        nativeController.getMeltingInventory().setStackInSlot(0, input.copy());
        helper.assertTrue(portable.melting().getRequiredTime(0) > 0
                && nativeController.getMeltingInventory().getRequiredTime(0) > 0, "Resolve a real melting recipe for " + input);
        if (freeSpace >= 0) {
            int occupied = portable.baseCapacity() - freeSpace;
            portable.controller().getTank().fill(new FluidStack(Fluids.WATER, occupied), FluidAction.EXECUTE);
            nativeController.getTank().fill(new FluidStack(Fluids.WATER, occupied), FluidAction.EXECUTE);
        }
        portable.refreshInputs();
        finish(portable.melting());
        finish(nativeController.getMeltingInventory());
        helper.assertTrue(portable.controller().getTank().getFluids().equals(nativeController.getTank().getFluids()),
                "Match actual native Transmute fluids and amounts for " + input + ", free=" + freeSpace);
        boolean completed = nativeController.getMeltingInventory().getStackInSlot(0).isEmpty();
        int initial = input.getCount() == 1 ? 1 : 64;
        helper.assertTrue(backpack.getStackInSlot(0).getCount() == initial - (completed ? input.getCount() : 0),
                "Consume only the completed native batch from the real backpack");
        SophisticatedSmeltery.LOGGER.info("ESTINKER OUTPUT: input={}, free={}, completed={}, fluids={}",
                input, freeSpace, completed, portable.controller().getTank().getFluids().stream()
                        .map(fluid -> ForgeRegistries.FLUIDS.getKey(fluid.getFluid()) + "=" + fluid.getAmount() + "mB").toList());
        return portable;
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, batch = "estinker_recipes")
    public static void defaultTransmuteOutputsAndCapacityMatchNative(GameTestHelper helper) {
        if (!ModList.get().isLoaded("esotericism_tinker")) { helper.succeed(); return; }
        for (var item : List.of(Items.IRON_INGOT, Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD)) {
            for (int count : new int[]{1, 2}) compareNative(helper, new ItemStack(item, count), -1);
        }
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        try {
            // Deterministic multi-byproduct recipes independently verify the swap and rounding rule.
            for (int main : new int[]{90, 91}) {
                String byproducts = main == 90
                        ? "[{\"fluid\":\"tconstruct:molten_copper\",\"amount\":30},{\"fluid\":\"tconstruct:molten_gold\",\"amount\":20}]"
                        : "[{\"fluid\":\"tconstruct:molten_copper\",\"amount\":2},{\"fluid\":\"tconstruct:molten_gold\",\"amount\":3}]";
                var json = JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:stick\"},\"result\":{\"fluid\":\"tconstruct:molten_iron\",\"amount\":"
                        + main + "},\"temperature\":800,\"time\":1,\"byproducts\":" + byproducts + "}").getAsJsonObject();
                manager.replaceRecipes(List.of(TinkerSmeltery.meltingSerializer.get().fromJson(SophisticatedSmeltery.id("transmute_rule"), json)));
                NativeFurnace.invalidateRecipes();
                for (int count : new int[]{1, 2}) {
                    var furnace = compareNative(helper, new ItemStack(Items.STICK, count), -1);
                    int copper = main == 90 ? 54 * count : main * count * 2 / 5;
                    int gold = main * count - copper;
                    int iron = (main == 90 ? 50 : 5) * count;
                    helper.assertTrue(furnace.controller().getTank().getFluids().equals(List.of(
                            new FluidStack(TinkerFluids.moltenIron.get(), iron),
                            new FluidStack(TinkerFluids.moltenCopper.get(), copper),
                            new FluidStack(TinkerFluids.moltenGold.get(), gold))),
                            "Swap total main/byproduct quantities; split proportionally and give remainder to largest byproduct");
                }
                compareNative(helper, new ItemStack(Items.STICK, 2), 0);
                compareNative(helper, new ItemStack(Items.STICK, 2), main * 2 - 1);
                var tight = compareNative(helper, new ItemStack(Items.STICK, 2), main * 2);
                int produced = tight.controller().getTank().getFluids().stream()
                        .filter(fluid -> fluid.getFluid() != Fluids.WATER).mapToInt(FluidStack::getAmount).sum();
                int expected = (main + (main == 90 ? 50 : 5)) * 2;
                if (produced != expected) SophisticatedSmeltery.LOGGER.info(
                        "ESTINKER NATIVE CAPACITY: main={}, free={}, unrestricted batch={}, actual={}, truncated={}; portable matches native",
                        main, main * 2, expected, produced, expected - produced);
            }
        } finally { manager.replaceRecipes(original); NativeFurnace.invalidateRecipes(); }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void optionalControllerLoadingAndPortableBaseline(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id("transmute_upgrade"));
        if (!ModList.get().isLoaded("esotericism_tinker")) {
            helper.assertTrue(recipe.isEmpty(), "Do not load the Transmute binding recipe without EsotericismTinker");
            SophisticatedSmeltery.LOGGER.info("ESTINKER BASELINE: absent, conditional recipe disabled");
            helper.succeed();
            return;
        }

        var controllerId = new ResourceLocation("esotericism_tinker", "transmute_controller");
        helper.assertTrue(ForgeRegistries.ITEMS.containsKey(controllerId), "Load the real Transmute controller item");
        ItemStack controller = new ItemStack(ForgeRegistries.ITEMS.getValue(controllerId));
        helper.assertTrue(recipe.isPresent() && ControllerBinding.supports(controller), "Enable the optional controller binding");
        FluidTank fuel = new FluidTank(4000);
        fuel.fill(new FluidStack(Fluids.LAVA, 4000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        NativeFurnace furnace = new NativeFurnace(controller, new CompoundTag(), () -> List.of(fuel), () -> {});
        helper.assertTrue(furnace.controller().getClass().getSimpleName().equals("TransmuteBlockEntity")
                && furnace.melting().getClass().getSimpleName().equals("SuperByproductMeltingModuleInventory"),
                "Use the real optional controller and its native melting inventory");
        furnace.resize(100, 1);
        var size = furnace.dimensions();
        helper.assertTrue(furnace.baseCapacity() == FluidValues.INGOT * 15 * (size.x() + 2) * (size.y() + 1) * (size.z() + 2),
                "Preserve the Transmute capacity override");
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        furnace.bindInputs(helper.getLevel(), inventory, new int[]{0});
        helper.assertTrue(furnace.melting().getSlotLimit(0) == 2, "Preserve the native two-item batch limit");
        for (int tick = 0; tick < 2400 && inventory.getStackInSlot(0).getCount() == 64; tick++) {
            furnace.refreshInputs();
            furnace.tick(true, tick);
        }
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 62 && furnace.controller().getTank().getContained() > 0,
                "Complete one native batch using fuel and consume exactly two real backpack items");
        NativeFurnace restored = new NativeFurnace(controller, furnace.save(), () -> List.of(fuel), () -> {});
        restored.resize(100, 1);
        restored.bindInputs(helper.getLevel(), inventory, new int[]{0});
        helper.assertTrue(restored.controller().getTank().getContained() == furnace.controller().getTank().getContained()
                && inventory.getStackInSlot(0).getCount() == 62, "Restore fluid without consuming or duplicating input");
        SophisticatedSmeltery.LOGGER.info("ESTINKER BASELINE: present, native controller batch and reload passed");
        helper.succeed();
    }
}
