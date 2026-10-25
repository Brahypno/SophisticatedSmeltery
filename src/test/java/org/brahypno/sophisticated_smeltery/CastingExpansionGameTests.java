package org.brahypno.sophisticated_smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import org.brahypno.sophisticated_smeltery.casting.PortableCasting;

@GameTestHolder("sophisticatedsmeltery")
@PrefixGameTestTemplate(false)
public final class CastingExpansionGameTests {
    private static ItemStack cast(String name) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("tconstruct", name)));
    }
    private static BackpackWrapper storage(boolean scorched, int iron) {
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.TANK_UPGRADE.get()));
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(scorched
                ? SophisticatedSmeltery.ADVANCED_SCORCHED_CASTING_UPGRADE.get() : SophisticatedSmeltery.ADVANCED_SEARED_CASTING_UPGRADE.get()));
        tank(storage).fill(new FluidStack(TinkerFluids.moltenIron.get(), iron), FluidAction.EXECUTE, true);
        return storage;
    }
    private static CastingUpgradeWrapper casting(BackpackWrapper storage) {
        return (CastingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(1);
    }
    private static TankUpgradeWrapper tank(BackpackWrapper storage) {
        return storage.getUpgradeHandler().getTypeWrappers(TankUpgradeItem.TYPE).iterator().next();
    }
    private static MeltingUpgradeWrapper furnace(BackpackWrapper storage) {
        return (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(2);
    }
    private static void addFurnace(BackpackWrapper storage, int iron) {
        storage.getUpgradeHandler().setStackInSlot(2, ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()),
                new ItemStack(TinkerSmeltery.smelteryController)));
        furnace(storage).getComponents().setStackInSlot(0, new ItemStack(TinkerSmeltery.searedBricks, 32));
        furnace(storage).setHeating(false);
        furnace(storage).getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), iron), FluidAction.EXECUTE);
        furnace(storage).persist();
    }
    private static void tick(GameTestHelper helper, CastingUpgradeWrapper casting, int count) {
        for (int tick = 0; tick < count; tick++) casting.tick(null, helper.getLevel(), BlockPos.ZERO);
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void standaloneSwitchPreservesReturnCoolingAndSavedSettings(GameTestHelper helper) {
        var storage = storage(false, 45);
        var casting = casting(storage);
        casting.casting().inventory().setStackInSlot(0, cast("ingot_cast"));
        casting.setAutomatic(true);
        tick(helper, casting, 100);
        helper.assertTrue(casting.controller().isEmpty() && casting.casting().fluid().getAmount() == 45, "Tank alone feeds partial casting");
        casting.setCastingEnabled(false);
        helper.assertTrue(casting.returnFluid(helper.getLevel()) == 45 && tank(storage).getContents().getAmount() == 45,
                "Disabled standalone casting can return partial liquid to the actual backpack tank");
        tank(storage).fill(new FluidStack(TinkerFluids.moltenIron.get(), 135), FluidAction.EXECUTE, false);
        tick(helper, casting, 60);
        helper.assertTrue(casting.pour(helper.getLevel()) == 0 && tank(storage).getContents().getAmount() == 180, "Off blocks automatic and manual pouring");
        casting.setCastingEnabled(true);
        tick(helper, casting, 10);
        casting.setCastingEnabled(false);
        casting.setOutputToStorage(true);
        storage.getUpgradeHandler().setStackInSlot(3, new ItemStack(SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get()));
        casting = casting(storage);
        helper.assertTrue(!casting.isCastingEnabled() && casting.isOutputToStorage() && !casting.canRemove(), "Persist switch, destination and busy removal lock across rebuild");
        tick(helper, casting, 100);
        helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).is(Items.IRON_INGOT)
                && tank(storage).getContents().getAmount() == 90 && casting.canRemove(), "Off still cools and delivers the existing batch, without starting another");
        casting.setCastingEnabled(true);
        tick(helper, casting, 60);
        helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).getCount() == 2 && tank(storage).getContents().isEmpty(),
                "Turning back on resumes tank-fed automatic casting and delivers the next batch");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void sourcePriorityAndOutputCapacityConserveFluid(GameTestHelper helper) {
        var storage = storage(false, 180);
        addFurnace(storage, 45);
        furnace(storage).getTank().fill(new FluidStack(TinkerFluids.moltenGold.get(), 90), FluidAction.EXECUTE);
        furnace(storage).persist();
        var casting = casting(storage);
        casting.casting().inventory().setStackInSlot(0, cast("ingot_cast"));
        helper.assertTrue(casting.pour(helper.getLevel()) == 90 && tank(storage).getContents().getAmount() == 135
                && furnace(storage).getTank().getContained() == 90, "Use bottom iron first, top up from tank without touching upper gold");
        tick(helper, casting, 60);
        helper.assertTrue(casting.pour(helper.getLevel()) == 90 && tank(storage).getContents().getAmount() == 45
                && furnace(storage).getTank().getContained() == 90,
                "Gold cannot mix with stacked iron, but matching iron from the tank remains usable");
        tick(helper, casting, 60);
        helper.assertTrue(casting.casting().inventory().getStackInSlot(1).getCount() == 2, "Both sources preserve exact output and fluid quantities");

        tank(storage).fill(new FluidStack(TinkerFluids.moltenIron.get(), 135), FluidAction.EXECUTE, true);
        var output = casting.casting().inventory();
        // Gold can use the bottom furnace fluid; use a foreign item to test rejection of all sources.
        output.setStackInSlot(1, new ItemStack(Items.DIAMOND));
        helper.assertTrue(casting.pour(helper.getLevel()) == 0, "Different result cannot overwrite output");
        ItemStack namedIron = new ItemStack(Items.IRON_INGOT);
        namedIron.getOrCreateTag().putInt("custom", 1);
        output.setStackInSlot(1, namedIron);
        helper.assertTrue(casting.pour(helper.getLevel()) == 0, "Different NBT cannot merge");
        output.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 64));
        helper.assertTrue(casting.pour(helper.getLevel()) == 0 && tank(storage).getContents().getAmount() == 180
                && furnace(storage).getTank().getContained() == 90, "Full result consumes neither source");
        output.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 63));
        helper.assertTrue(casting.pour(helper.getLevel()) == 90, "Accept exactly remaining stack space from matching source");
        tick(helper, casting, 60);
        helper.assertTrue(output.getStackInSlot(1).getCount() == 64 && tank(storage).getContents().getAmount() == 90, "Merge without overflow");
        output.extractItem(1, 1, false);
        helper.assertTrue(casting.pour(helper.getLevel()) == 90 && output.extractItem(1, 63, false).getCount() == 63,
                "Old output remains removable during the next cooldown");
        helper.assertTrue(output.extractItem(0, 1, false).isEmpty(), "Active mold stays locked");
        tick(helper, casting, 60);
        helper.assertTrue(output.getStackInSlot(1).getCount() == 1 && tank(storage).getContents().isEmpty(), "Next batch finishes once");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void outputDeliveryHandlesFullStorageAndDynamicResults(GameTestHelper helper) {
        var storage = storage(false, 270);
        addFurnace(storage, 0);
        var casting = casting(storage);
        casting.casting().inventory().setStackInSlot(0, cast("ingot_cast"));
        casting.setOutputToStorage(true);
        int count = furnace(storage).getMeltingSlotCount();
        helper.assertTrue(count > 0 && count < storage.getInventoryHandler().getSlots(), "Fixture has both melting and ordinary slots");
        storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        casting.pour(helper.getLevel());
        tick(helper, casting, 60);
        helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).getCount() == 1
                && storage.getInventoryHandler().getStackInSlot(count).is(Items.IRON_INGOT), "Empty ordinary space wins over a matching melting stack");
        for (int slot = count; slot < storage.getInventoryHandler().getSlots(); slot++)
            storage.getInventoryHandler().setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        casting.pour(helper.getLevel());
        tick(helper, casting, 60);
        helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).getCount() == 2, "Use melting slots as fallback when ordinary slots cannot accept output");
        for (int slot = 0; slot < storage.getInventoryHandler().getSlots(); slot++)
            storage.getInventoryHandler().setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        casting.pour(helper.getLevel());
        tick(helper, casting, 60);
        helper.assertTrue(casting.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT), "A full backpack retains the paid result");
        storage.getInventoryHandler().setStackInSlot(0, ItemStack.EMPTY);
        tick(helper, casting, 1);
        helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).is(Items.IRON_INGOT)
                && casting.casting().inventory().getStackInSlot(1).isEmpty(), "Waiting output enters newly freed space exactly once");
        for (boolean switched : new boolean[]{false, true}) {
            var dynamicStorage = storage(false, 0);
            tank(dynamicStorage).fill(new FluidStack(switched ? TinkerFluids.moltenGold.get() : TinkerFluids.moltenIron.get(), 90), FluidAction.EXECUTE, true);
            var dynamicCasting = casting(dynamicStorage);
            dynamicCasting.setMode(switched ? PortableCasting.Mode.TABLE : PortableCasting.Mode.BASIN);
            ItemStack input = switched ? new ItemStack(Items.IRON_INGOT)
                    : new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.INGOT_TANK));
            input.getOrCreateTag().putInt("retained", 1729);
            dynamicCasting.casting().inventory().setStackInSlot(0, input);
            dynamicCasting.setOutputToStorage(true);
            helper.assertTrue(dynamicCasting.pour(helper.getLevel()) == 90, "Use the native recipe's actual amount");
            tick(helper, dynamicCasting, 60);
            ItemStack result = dynamicStorage.getInventoryHandler().getStackInSlot(0);
            if (switched) helper.assertTrue(result.is(cast("ingot_cast").getItem()), "Deliver the switchSlots result, consuming the ingot template");
            else {
                var handler = result.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new);
                helper.assertTrue(result.getTag().getInt("retained") == 1729 && handler.getFluidInTank(0).getAmount() == 90,
                        "Preserve the result's dynamic fluid and custom NBT");
            }
            helper.assertTrue(dynamicCasting.casting().inventory().getStackInSlot(0).isEmpty() && dynamicCasting.casting().inventory().getStackInSlot(1).isEmpty()
                    && tank(dynamicStorage).getContents().isEmpty(), "Deliver exactly once, without leaving a duplicate template or fluid");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void scorchedLockRefillsExactConsumedTemplateAndSurvivesReload(GameTestHelper helper) {
        var storage = storage(true, 270);
        var casting = casting(storage);
        ItemStack template = cast("ingot_sand_cast");
        template.getOrCreateTag().putInt("template", 7);
        casting.casting().inventory().setStackInSlot(0, template.copy());
        casting.casting().toggleCastLock();
        storage.getInventoryHandler().setStackInSlot(0, cast("ingot_sand_cast"));
        storage.getInventoryHandler().setStackInSlot(1, template.copyWithCount(2));
        casting.setAutomatic(true);
        storage.getUpgradeHandler().setStackInSlot(3, new ItemStack(SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get()));
        casting = casting(storage);
        helper.assertTrue(ItemStack.isSameItemSameTags(template, casting.casting().lockedCast()), "Lock survives a real wrapper rebuild");
        helper.assertTrue(!casting.casting().inventory().isItemValid(0, cast("ingot_cast")), "Locked slot refuses another mold");
        tick(helper, casting, 200);
        helper.assertTrue(casting.casting().inventory().getStackInSlot(1).getCount() == 3 && tank(storage).getContents().isEmpty()
                && storage.getInventoryHandler().getStackInSlot(1).isEmpty() && storage.getInventoryHandler().getStackInSlot(0).getCount() == 1,
                "Three casts consume exactly three matching templates, leaving the different-NBT template untouched");
        tank(storage).fill(new FluidStack(TinkerFluids.moltenIron.get(), 90), FluidAction.EXECUTE, false);
        tick(helper, casting, 60);
        helper.assertTrue(tank(storage).getContents().getAmount() == 90, "Missing locked template waits without consuming an unrelated item");
        storage.getInventoryHandler().setStackInSlot(1, template.copy());
        tick(helper, casting, 60);
        helper.assertTrue(casting.casting().inventory().getStackInSlot(1).getCount() == 4, "Adding a matching template resumes automatically");
        helper.succeed();
    }
}
