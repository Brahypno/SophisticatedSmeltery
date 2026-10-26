package org.brahypno.sophisticated_smeltery;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.furnace.NativeFurnace;
import org.brahypno.sophisticated_smeltery.recipe.ControllerCraftingRefunds;
import org.brahypno.sophisticated_smeltery.recipe.TransmuteTierRecipe;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

@PrefixGameTestTemplate(false)
public final class TransmuteControlsGameTests {
    private static ItemStack item(String name) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("esotericism_tinker", name)));
    }
    private static BackpackWrapper storage(boolean advanced) {
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, ControllerBinding.bind(new ItemStack(advanced
                ? SophisticatedSmeltery.ADVANCED_TRANSMUTE_UPGRADE.get() : SophisticatedSmeltery.MELTING_UPGRADE.get()), item("transmute_controller")));
        var wrapper = (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
        wrapper.setHeating(false);
        wrapper.getComponents().setStackInSlot(0, item("ashen_bricks").copyWithCount(64));
        wrapper.getComponents().setStackInSlot(1, item("ashen_fuel_tank"));
        wrapper.getComponents().setStackInSlot(2, item("ashen_heater").copyWithCount(2));
        return storage;
    }
    private static MeltingUpgradeWrapper wrapper(BackpackWrapper storage) {
        return (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void tierRecipeLayoutStateNetworkAndNoDuplicateRefund(GameTestHelper helper) {
        var optional = helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id("advanced_transmute_upgrade"));
        if (!ModList.get().isLoaded("esotericism_tinker")) {
            helper.assertTrue(optional.isEmpty(), "The advanced recipe stays disabled without the optional mod");
            helper.succeed(); return;
        }
        var recipe = (TransmuteTierRecipe)optional.orElseThrow();
        var storage = storage(false);
        var original = wrapper(storage);
        original.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 90), FluidAction.EXECUTE);
        original.getContainers().setStackInSlot(1, new ItemStack(Items.BUCKET));
        original.persist();
        var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(Player player) { return true; }
        }, 3, 3);
        grid.setItem(0, item("ashen_alloy_switch"));
        grid.setItem(1, new ItemStack(Items.DIAMOND));
        grid.setItem(2, item("ashen_melt_switch"));
        grid.setItem(3, new ItemStack(Items.GOLD_INGOT));
        grid.setItem(4, original.getUpgradeStack().copy());
        grid.setItem(5, new ItemStack(Items.GOLD_INGOT));
        for (int slot = 6; slot < 9; slot++) grid.setItem(slot, new ItemStack(Items.REDSTONE));
        helper.assertTrue(recipe.matches(grid, helper.getLevel()) && !recipe.isSpecial() && recipe.getIngredients().size() == 9,
                "Use A-D-M / G-U-G / R-R-R and expose the shaped recipe to JEI");
        helper.assertTrue(ControllerBinding.getController(recipe.getIngredients().get(4).getItems()[0]).is(item("transmute_controller").getItem()),
                "The recipe ingredient shows a bound standard Transmute upgrade");
        var result = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(SophisticatedSmeltery.ADVANCED_TRANSMUTE_UPGRADE.get())
                        && result.getTag().equals(original.getUpgradeStack().getTag())
                        && recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty),
                "Upgrade a filled furnace without losing or refunding its controller, tank, materials or container");
        var player = helper.makeMockPlayer();
        ControllerCraftingRefunds.crafted(new PlayerEvent.ItemCraftedEvent(player, result, grid));
        helper.assertTrue(player.getInventory().isEmpty(), "State-preserving tier crafting must not also refund stored items");
        var upgraded = new MeltingUpgradeWrapper(storage, result, ignored -> {});
        helper.assertTrue(upgraded.hasTransmuteControls() && upgraded.getTank().getContained() == 90
                && upgraded.getComponents().getStackInSlot(0).getCount() == 64, "Restore the upgraded filled furnace");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SophisticatedSmeltery.TRANSMUTE_TIER.get().toNetwork(buffer, recipe);
            var decoded = SophisticatedSmeltery.TRANSMUTE_TIER.get().fromNetwork(recipe.getId(), buffer);
            helper.assertTrue(decoded.matches(grid, helper.getLevel())
                    && ItemStack.matches(decoded.getResultItem(helper.getLevel().registryAccess()), recipe.getResultItem(helper.getLevel().registryAccess())),
                    "The client receives the same ingredients and bound result");
        } finally { buffer.release(); }
        for (var invalid : List.of(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), ControllerBinding.bind(
                new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), new ItemStack(TinkerSmeltery.smelteryController)))) {
            grid.setItem(4, invalid);
            helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "Reject blank upgrades and other furnace types");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, batch = "transmute_controls")
    public static void threeNativeModesAlloyGateResizeReloadAndBasicTier(GameTestHelper helper) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("esotericism_tinker")) { helper.succeed(); return; }
        var storage = storage(true);
        var wrapper = wrapper(storage);
        var container = new MeltingUpgradeContainer(helper.makeMockPlayer(), 0, wrapper, SophisticatedSmeltery.MELTING_CONTAINER);
        var message = new CompoundTag();
        var manager = helper.getLevel().getRecipeManager();
        var recipes = new ArrayList<>(manager.getRecipes());
        try {
            var json = JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:stick\"},\"result\":{\"fluid\":\"tconstruct:molten_iron\",\"amount\":90},\"temperature\":100,\"time\":1,\"byproducts\":[{\"fluid\":\"tconstruct:molten_gold\",\"amount\":30}]}").getAsJsonObject();
            manager.replaceRecipes(List.of(TinkerSmeltery.meltingSerializer.get().fromJson(SophisticatedSmeltery.id("mode_rule"), json)));
            for (int mode = 0; mode < 3; mode++) {
                helper.assertTrue(wrapper.getTransmuteMode() == mode, "Cycle exactly the three requested modes");
                wrapper.getTank().setFluids(List.of());
                storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.STICK, 2));
                wrapper.tick(null, helper.getLevel(), BlockPos.ZERO);
                wrapper.getFurnace().refreshInputs();
                var nativeController = ControllerBinding.create(item("transmute_controller"));
                nativeController.setLevel(helper.getLevel());
                nativeController.getTank().setCapacity(wrapper.getTank().getCapacity());
                var inv = nativeController.getMeltingInventory();
                var enumType = Class.forName("org.brahypno.esotericismtinker.smeltery.block.entity.module.SuperByproductMeltingModuleInventory$MeltType");
                inv.getClass().getMethod("updateMeltType", enumType).invoke(inv, enumType.getEnumConstants()[mode]);
                inv.resize(1, ignored -> {});
                inv.setStackInSlot(0, new ItemStack(Items.STICK, 2));
                for (int pass = 0; pass < 2; pass++) {
                    wrapper.getFurnace().melting().heatItems(3000, 100000);
                    inv.heatItems(3000, 100000);
                }
                helper.assertTrue(wrapper.getTank().getFluids().equals(nativeController.getTank().getFluids())
                        && storage.getInventoryHandler().getStackInSlot(0).isEmpty(), "Each mode uses native outputs and consumes only its batch");
                message.putString("action", "transmuteMode"); container.handleMessage(message);
            }
        } finally { manager.replaceRecipes(recipes); NativeFurnace.invalidateRecipes(); }
        helper.assertTrue(wrapper.getTransmuteMode() == 0, "Wrap to Transmute without enabling the unused fourth native enum");
        wrapper.getTank().setFluids(List.of());
        wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenCopper.get(), 270), FluidAction.EXECUTE);
        wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenGold.get(), 270), FluidAction.EXECUTE);
        wrapper.tick(null, helper.getLevel(), BlockPos.ZERO);
        var before = wrapper.getTank().getFluids().stream().map(FluidStack::copy).toList();
        wrapper.getFurnace().tick(true, 2);
        helper.assertTrue(wrapper.getTank().getFluids().equals(before), "Alloying defaults to off");
        message.putString("action", "transmuteAlloying"); container.handleMessage(message);
        wrapper.getFurnace().tick(false, 2);
        helper.assertTrue(wrapper.getTank().getFluids().equals(before), "The master heating switch also gates alloying");
        message.putString("action", "transmuteMode"); container.handleMessage(message);
        wrapper.getComponents().setStackInSlot(0, item("ashen_bricks").copyWithCount(63));
        wrapper.persist();
        var restored = new MeltingUpgradeWrapper(storage, wrapper.getUpgradeStack().copy(), ignored -> {});
        restored.tick(null, helper.getLevel(), BlockPos.ZERO);
        helper.assertTrue(restored.getTransmuteMode() == 1 && restored.isTransmuteAlloying(), "Retain both switches after structure resize and reconstruction");
        restored.getFurnace().tick(true, 2);
        helper.assertTrue(restored.getTank().getFluids().stream().anyMatch(fluid -> fluid.getFluid() == TinkerFluids.moltenRoseGold.get()),
                "The enabled native alloy module actually produces rose gold after reload");
        restored.toggleTransmuteAlloying();
        var stopped = restored.getTank().getFluids().stream().map(FluidStack::copy).toList();
        restored.getFurnace().tick(true, 2);
        helper.assertTrue(restored.getTank().getFluids().equals(stopped), "Turning alloying off immediately stops the native module");
        var basic = wrapper(storage(false));
        basic.cycleTransmuteMode(); basic.toggleTransmuteAlloying();
        helper.assertTrue(!basic.hasTransmuteControls() && basic.getTransmuteMode() == 0 && !basic.isTransmuteAlloying(),
                "The standard upgrade keeps the native default and rejects advanced control actions");
        SophisticatedSmeltery.LOGGER.info("TRANSMUTE CONTROLS PASSED: three native modes, alloy off/on/off, master heating gate, resize, reload and tier restrictions");
        helper.succeed();
    }
}
