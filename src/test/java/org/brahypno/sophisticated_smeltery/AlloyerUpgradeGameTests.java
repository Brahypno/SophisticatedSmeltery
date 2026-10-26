package org.brahypno.sophisticated_smeltery;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.furnace.PortableAlloyer;
import org.brahypno.sophisticated_smeltery.recipe.UpgradeClearingRecipe;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeWrapper;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipeBuilder;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;

@PrefixGameTestTemplate(false)
public final class AlloyerUpgradeGameTests {
    private static BackpackWrapper backpack() {
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(SophisticatedSmeltery.ALLOYER_UPGRADE.get()));
        return storage;
    }
    private static AlloyerUpgradeWrapper alloyer(BackpackWrapper storage) {
        return (AlloyerUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
    }
    private static ItemStack tank(Fluid fluid, int amount) {
        ItemStack stack = new ItemStack(TinkerSmeltery.scorchedTank.get(TankType.INGOT_TANK));
        if (amount > 0) stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new).fill(new FluidStack(fluid, amount), FluidAction.EXECUTE);
        return stack;
    }
    private static void roseGold(AlloyerUpgradeWrapper wrapper) {
        wrapper.getComponents().setStackInSlot(6, tank(TinkerFluids.moltenCopper.get(), 90));
        wrapper.getComponents().setStackInSlot(7, tank(TinkerFluids.moltenGold.get(), 90));
    }
    private static void heat(GameTestHelper helper, AlloyerUpgradeWrapper wrapper, int ticks) {
        wrapper.alloyer().bind(helper.getLevel());
        for (int tick = 0; tick < ticks; tick++) wrapper.alloyer().tick(wrapper.isHeating(), tick);
        wrapper.persist();
    }
    private static CompoundTag action(String action) { CompoundTag data = new CompoundTag(); data.putString("action", action); return data; }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void independentUpgradeCoexistsAndKeepsNineComponentSlots(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = alloyer(storage);
        var controller = ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), new ItemStack(TinkerSmeltery.foundryController));
        helper.assertTrue(storage.getUpgradeHandler().insertItem(1, controller, false).isEmpty(), "Install a normal furnace alongside the independent alloyer");
        helper.assertTrue(wrapper.getUpgradeStack().is(ModItems.BACKPACK_UPGRADE_TAG), "Standalone upgrade is accepted by normal backpack slots");
        helper.assertTrue(!ControllerBinding.supports(new ItemStack(TinkerSmeltery.scorchedAlloyer)), "The alloyer does not become a melting controller");
        helper.assertTrue(wrapper.getComponents().getSlots() == 9 && wrapper.getContainers().getSlots() == 2, "Keep the existing 3x3 component grid and bucket row");
        for (int slot = 0; slot < 8; slot++) {
            helper.assertTrue(wrapper.getComponents().insertItem(slot, tank(Fluids.WATER, 0), false).isEmpty(), "Accept ingredient tank " + slot);
            helper.assertTrue(wrapper.getComponents().getSlotLimit(slot) == 1 && !wrapper.getComponents().isItemValid(slot, new ItemStack(Items.COAL)), "Ingredient slots contain one tank and do not burn it");
        }
        helper.assertTrue(wrapper.getComponents().insertItem(8, new ItemStack(Items.COAL, 16), false).isEmpty(), "The ninth slot is reserved for fuel");
        helper.assertTrue(wrapper.getTank().getCapacity() == TinkerSmeltery.alloyer.get().create(BlockPos.ZERO,
                TinkerSmeltery.scorchedAlloyer.get().defaultBlockState()).getTank().getCapacity(), "Built-in capacity is native and independent of installed tanks");
        helper.assertTrue(SophisticatedSmeltery.MELTING_UPGRADE.get().canAddUpgradeTo(storage, controller, true, false).isSuccessful() == false,
                "The existing one-melting-controller restriction still applies to a second furnace");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void nativeAlloyConsumesLastInputsUsesSolidFuelAndPauses(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = alloyer(storage);
        roseGold(wrapper);
        wrapper.getComponents().setStackInSlot(8, new ItemStack(Items.COAL, 2));
        wrapper.setHeating(false);
        heat(helper, wrapper, 8);
        helper.assertTrue(wrapper.getTank().isEmpty() && wrapper.getInputTank(7).getFluidInTank(0).getAmount() == 90, "Off switch prevents ingredient and fuel consumption");
        wrapper.setHeating(true);
        heat(helper, wrapper, 4);
        helper.assertTrue(wrapper.getTank().getFluid().getFluid() == TinkerFluids.moltenRoseGold.get() && wrapper.getTank().getFluidAmount() == 180,
                "Native rose gold recipe reads the seventh and eighth tanks and keeps exact output amounts");
        helper.assertTrue(wrapper.getInputTank(6).getFluidInTank(0).isEmpty() && wrapper.getInputTank(7).getFluidInTank(0).isEmpty()
                && wrapper.getComponents().getStackInSlot(8).getCount() == 1, "Consume only native recipe inputs and one coal");
        int remaining = wrapper.alloyer().fuel().getFuel();
        wrapper.setHeating(false);
        heat(helper, wrapper, 20);
        helper.assertTrue(wrapper.alloyer().fuel().getFuel() == remaining, "Paused upgrade retains its paid burn time");
        var reloaded = new AlloyerUpgradeWrapper(storage, wrapper.getUpgradeStack().copy(), ignored -> {});
        helper.assertTrue(!reloaded.isHeating() && reloaded.alloyer().fuel().getFuel() == remaining
                && reloaded.getTank().getFluidAmount() == 180 && reloaded.getComponents().getStackInSlot(8).getCount() == 1, "Reload preserves output, fuel items, burn and switch");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, batch = "alloyer_recipes")
    public static void eightIngredientRecipeReloadAndFullOutputGuard(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        var recipes = new ArrayList<>(original);
        recipes.removeIf(recipe -> recipe.getType() == TinkerRecipeTypes.ALLOYING.get());
        Fluid[] fluids = {Fluids.WATER, Fluids.LAVA, TinkerFluids.moltenIron.get(), TinkerFluids.moltenGold.get(),
                TinkerFluids.moltenCopper.get(), TinkerFluids.moltenCobalt.get(), TinkerFluids.searedStone.get(), TinkerFluids.scorchedStone.get()};
        var builder = AlloyRecipeBuilder.alloy(TinkerFluids.moltenRoseGold, 80);
        for (Fluid fluid : fluids) builder.addInput(fluid, 10);
        builder.save(finished -> recipes.add(TinkerSmeltery.alloyingSerializer.get().fromJson(finished.getId(), finished.serializeRecipe())), SophisticatedSmeltery.id("eight_inputs"));
        try {
            manager.replaceRecipes(recipes);
            PortableAlloyer.invalidateRecipes();
            var storage = backpack();
            var wrapper = alloyer(storage);
            for (int slot = 0; slot < 8; slot++) wrapper.getComponents().setStackInSlot(slot, tank(fluids[slot], 20));
            wrapper.getComponents().setStackInSlot(8, tank(Fluids.LAVA, 1000));
            wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), wrapper.getTank().getCapacity()), FluidAction.EXECUTE);
            heat(helper, wrapper, 4);
            helper.assertTrue(wrapper.getInputTank(7).getFluidInTank(0).getAmount() == 20 && wrapper.alloyer().fuel().getFuel() == 0,
                    "Full or incompatible output prevents ignition and ingredient consumption");
            wrapper.getTank().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
            wrapper.alloyer().fuel().set(0, 100);
            wrapper.alloyer().fuel().set(2, 3000);
            heat(helper, wrapper, 4);
            helper.assertTrue(wrapper.getTank().getFluidAmount() == 80, "Native module supports an actual eight-ingredient recipe");
            for (int slot = 0; slot < 8; slot++) helper.assertTrue(wrapper.getInputTank(slot).getFluidInTank(0).getAmount() == 10, "Each of eight tanks contributes exactly once");
            int before = wrapper.getTank().getFluidAmount();
            manager.replaceRecipes(original);
            PortableAlloyer.invalidateRecipes();
            wrapper.alloyer().bind(helper.getLevel());
            heat(helper, wrapper, 4);
            helper.assertTrue(wrapper.getTank().getFluidAmount() == before && wrapper.getInputTank(7).getFluidInTank(0).getAmount() == 10,
                    "Reload discards the cached removed recipe without consuming its old inputs");
        } finally { manager.replaceRecipes(original); PortableAlloyer.invalidateRecipes(); }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void builtInTankBucketsInputSelectionAndMessages(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = alloyer(storage);
        wrapper.setHeating(false);
        wrapper.getContainers().setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(wrapper.getTank().getFluidAmount() == 1000 && wrapper.getContainers().getStackInSlot(1).is(Items.BUCKET), "Bucket row fills the built-in tank without components");
        wrapper.getContainers().setStackInSlot(1, ItemStack.EMPTY);
        wrapper.getContainers().setStackInSlot(0, new ItemStack(Items.BUCKET));
        helper.assertTrue(wrapper.getTank().isEmpty() && wrapper.getContainers().getStackInSlot(1).is(Items.WATER_BUCKET), "Bucket row drains the built-in tank");
        wrapper.getContainers().setStackInSlot(1, ItemStack.EMPTY);
        wrapper.getComponents().setStackInSlot(7, tank(Fluids.WATER, 0));
        for (int i = 0; i < 8; i++) wrapper.cycleContainerTarget();
        helper.assertTrue(wrapper.getContainerTarget() == 8, "All eight input tanks are selectable");
        wrapper.getContainers().setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(wrapper.getInputTank(7).getFluidInTank(0).getAmount() == 1000 && wrapper.getTank().isEmpty(), "Selected input receives the container contents");
        var player = helper.makeMockPlayer();
        player.containerMenu.setCarried(new ItemStack(Items.BUCKET));
        var container = new AlloyerUpgradeContainer(player, 0, wrapper, SophisticatedSmeltery.ALLOYER_CONTAINER);
        var data = action("fluid"); data.putInt("target", 8);
        container.handleMessage(data);
        helper.assertTrue(player.containerMenu.getCarried().is(Items.WATER_BUCKET) && wrapper.getInputTank(7).getFluidInTank(0).isEmpty(), "Direct input-tank click uses the authoritative server handler");
        var saved = new AlloyerUpgradeWrapper(storage, wrapper.getUpgradeStack().copy(), ignored -> {});
        helper.assertTrue(saved.getContainerTarget() == 8 && saved.getInputTank(7).getFluidInTank(0).isEmpty(), "Container selection and fluid changes save immediately");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void fluidCapabilityCastingAndRemovalStayIndependent(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = alloyer(storage);
        wrapper.setHeating(false);
        var fluid = wrapper.wrapHandler(null, storage.getBackpack());
        var iron = new FluidStack(TinkerFluids.moltenIron.get(), 90);
        helper.assertTrue(fluid.fill(iron, FluidAction.SIMULATE) == 90 && wrapper.getTank().isEmpty(), "Simulated pipe input does not change storage");
        fluid.fill(iron, FluidAction.EXECUTE);
        helper.assertTrue(!wrapper.canRemove() && storage.getUpgradeHandler().extractItem(0, 1, true).isEmpty()
                && storage.getUpgradeHandler().extractItem(0, 1, false).isEmpty(), "Real and simulated upgrade extraction preserve alloyer contents");
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get()));
        wrapper = alloyer(storage);
        var casting = (CastingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(1);
        casting.casting().inventory().setStackInSlot(0, new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("tconstruct", "ingot_cast"))));
        helper.assertTrue(casting.pour(helper.getLevel()) == 90 && wrapper.getTank().isEmpty(), "Casting can consume alloyer output without a melting controller or backpack tank");
        wrapper.getComponents().setStackInSlot(0, tank(Fluids.WATER, 100));
        helper.assertTrue(!wrapper.canRemove(), "Ingredient fluids also prevent removing the upgrade");
        wrapper.getInputTank(0).drain(100, FluidAction.EXECUTE);
        wrapper.getComponents().setStackInSlot(8, tank(Fluids.LAVA, 100));
        helper.assertTrue(!wrapper.canRemove(), "Internal fuel fluids prevent removal");
        wrapper.getInternalFuelTanks().get(0).drain(100, FluidAction.EXECUTE);
        helper.assertTrue(wrapper.canRemove() && !storage.getUpgradeHandler().extractItem(0, 1, false).isEmpty(), "Empty alloyer is removable independently of casting contents");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void placedAlloyerCapabilityAndLiquidFuel(GameTestHelper helper) {
        var storage = backpack();
        helper.setBlock(BlockPos.ZERO, ModBlocks.NETHERITE_BACKPACK.get());
        var block = (BackpackBlockEntity)helper.getBlockEntity(BlockPos.ZERO);
        block.setBackpack(storage.getBackpack());
        var wrapper = alloyer((BackpackWrapper)block.getBackpackWrapper());
        roseGold(wrapper);
        wrapper.getComponents().setStackInSlot(8, tank(Fluids.LAVA, 1000));
        heat(helper, wrapper, 4);
        helper.assertTrue(wrapper.getTank().getFluidAmount() == 180 && wrapper.getInternalFuelTanks().get(0).getFluidInTank(0).getAmount() < 1000,
                "Internal liquid fuel powers the native alloy recipe");
        IFluidHandler handler = block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElseThrow(IllegalStateException::new);
        helper.assertTrue(handler.drain(180, FluidAction.SIMULATE).getAmount() == 180 && wrapper.getTank().getFluidAmount() == 180, "Placed backpack advertises alloyer output");
        handler.drain(180, FluidAction.EXECUTE);
        var loaded = new AlloyerUpgradeWrapper(storage, wrapper.getUpgradeStack().copy(), ignored -> {});
        helper.assertTrue(loaded.getTank().isEmpty(), "Pipe extraction saves the real output without waiting for a tick");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void clearingKeepsInstalledTankItemsAndRejectsOutputFluid(GameTestHelper helper) {
        var storage = backpack();
        var wrapper = alloyer(storage);
        wrapper.getComponents().setStackInSlot(7, tank(Fluids.WATER, 1000));
        wrapper.getComponents().setStackInSlot(8, new ItemStack(Items.COAL, 3));
        var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(Player player) { return true; }
        }, 2, 2);
        var recipe = (UpgradeClearingRecipe)helper.getLevel().getRecipeManager().byKey(new ResourceLocation("sophisticatedcore", "upgrade_clear")).orElseThrow();
        grid.setItem(0, wrapper.getUpgradeStack().copy());
        helper.assertTrue(recipe.matches(grid, helper.getLevel()) && AlloyerUpgradeWrapper.storedItems(grid.getItem(0)).size() == 2,
                "Clearing an empty output returns ingredient tanks with their fluid and fuel items");
        var cleared = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(cleared.is(SophisticatedSmeltery.ALLOYER_UPGRADE.get()) && !cleared.hasTag(), "Clearing returns the standalone alloyer upgrade");
        wrapper.getTank().fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        wrapper.persist();
        grid.setItem(0, wrapper.getUpgradeStack().copy());
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "Clearing cannot erase the built-in tank's contents");
        helper.succeed();
    }
}
