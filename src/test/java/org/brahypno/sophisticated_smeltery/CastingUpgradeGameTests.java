package org.brahypno.sophisticated_smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import org.brahypno.sophisticated_smeltery.casting.PortableCasting;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

@GameTestHolder("sophisticatedsmeltery")
@PrefixGameTestTemplate(false)
public final class CastingUpgradeGameTests {
    private static BackpackWrapper backpack(int amount) {
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()),
                new ItemStack(TinkerSmeltery.smelteryController)));
        var furnace = furnace(storage);
        furnace.getComponents().setStackInSlot(0, new ItemStack(TinkerSmeltery.searedBricks, 32));
        furnace.setHeating(false);
        furnace.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), amount), FluidAction.EXECUTE);
        furnace.persist();
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(SophisticatedSmeltery.CASTING_UPGRADE.get()));
        casting(storage, 1).casting().inventory().setStackInSlot(0,
                new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("tconstruct", "ingot_cast"))));
        return storage;
    }
    private static MeltingUpgradeWrapper furnace(BackpackWrapper storage) {
        return (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
    }
    private static CastingUpgradeWrapper casting(BackpackWrapper storage, int slot) {
        return (CastingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(slot);
    }
    private static void tick(GameTestHelper helper, CastingUpgradeWrapper casting, int count) {
        for (int tick = 0; tick < count; tick++) casting.tick(null, helper.getLevel(), BlockPos.ZERO);
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void manualCastingAndCoolingWorkWithHeatingOff(GameTestHelper helper) {
        var storage = backpack(180);
        var casting = casting(storage, 1);
        tick(helper, casting, 10);
        helper.assertTrue(!casting.isAutomatic() && furnace(storage).getTank().getContained() == 180,
                "A new upgrade defaults to manual and cannot pour just because it is installed");
        var container = new CastingUpgradeContainer(helper.makeMockPlayer(), 1, casting, SophisticatedSmeltery.CASTING_CONTAINER);
        CompoundTag message = new CompoundTag();
        message.putString("action", "pour");
        container.handleMessage(message);
        helper.assertTrue(!furnace(storage).isHeating() && casting.casting().isCooling() && furnace(storage).getTank().getContained() == 90,
                "Manual GUI message uses existing furnace fluid despite heating being off");
        tick(helper, casting, 60);
        helper.assertTrue(casting.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT), "Cooling completes while heating stays off");
        helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).isEmpty(), "Independent output must not enter a melting input");
        var loaded = new CastingUpgradeWrapper(storage, casting.getUpgradeStack().copy(), ignored -> {});
        helper.assertTrue(loaded.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT) && loaded.canRemove(),
                "Persist finished output and allow removing the clean upgrade with its items");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void automaticCastingWaitsForOutputAndRestartsAfterPickup(GameTestHelper helper) {
        var storage = backpack(180);
        var casting = casting(storage, 1);
        casting.setAutomatic(true);
        tick(helper, casting, 60);
        helper.assertTrue(casting.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT) && furnace(storage).getTank().getContained() == 90,
                "Automatic mode casts one item and retains the second recipe behind an occupied output");
        tick(helper, casting, 20);
        helper.assertTrue(furnace(storage).getTank().getContained() == 90, "Occupied output cannot consume more furnace fluid");
        ItemStack first = casting.casting().inventory().extractItem(1, 1, false);
        tick(helper, casting, 60);
        helper.assertTrue(first.is(Items.IRON_INGOT) && casting.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT)
                && furnace(storage).getTank().getContained() == 0, "Taking output triggers exactly one subsequent automatic recipe");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void busyUpgradeBlocksOnlyItsOwnExtractionAndSwap(GameTestHelper helper) {
        var storage = backpack(45);
        storage.getUpgradeHandler().setStackInSlot(2, new ItemStack(SophisticatedSmeltery.CASTING_UPGRADE.get()));
        var busy = casting(storage, 1);
        helper.assertTrue(SophisticatedSmeltery.CASTING_UPGRADE.get().canAddUpgradeTo(storage,
                new ItemStack(SophisticatedSmeltery.CASTING_UPGRADE.get()), true, false).isSuccessful(), "Multiple casting upgrades have no added count limit");
        busy.pour(helper.getLevel());
        helper.assertTrue(!busy.canRemove() && !busy.casting().isCooling(), "Partial fluid locks removal even before cooling starts");
        helper.assertTrue(storage.getUpgradeHandler().extractItem(1, 1, true).isEmpty()
                && storage.getUpgradeHandler().extractItem(1, 1, false).isEmpty(), "Both simulated and actual extraction must keep a filled upgrade installed");
        helper.assertTrue(!SophisticatedSmeltery.CASTING_UPGRADE.get().canSwapUpgradeFor(
                new ItemStack(SophisticatedSmeltery.CASTING_UPGRADE.get()), 1, storage, false).isSuccessful(), "A filled upgrade cannot be bypassed by same-item swap");
        helper.assertTrue(!storage.getUpgradeHandler().extractItem(2, 1, false).isEmpty(), "Another empty casting upgrade remains independently removable");
        busy = casting(storage, 1); // The native upgrade handler rebuilds wrappers after another slot is removed.
        helper.assertTrue(busy.returnFluid(helper.getLevel()) == 45 && busy.canRemove(), "Return partial fluid before removal");
        helper.assertTrue(!storage.getUpgradeHandler().extractItem(1, 1, false).isEmpty(), "The same upgrade unlocks after fluid is returned");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void coolingUpgradeStaysLockedAcrossReloadAndReturnMessage(GameTestHelper helper) {
        var storage = backpack(90);
        var casting = casting(storage, 1);
        casting.pour(helper.getLevel());
        tick(helper, casting, 20);
        var restored = new CastingUpgradeWrapper(storage, casting.getUpgradeStack().copy(), ignored -> {});
        helper.assertTrue(!restored.canRemove() && restored.casting().timer() == 20, "Reloaded full fluid locks removal before its first tick");
        var container = new CastingUpgradeContainer(helper.makeMockPlayer(), 1, restored, SophisticatedSmeltery.CASTING_CONTAINER);
        CompoundTag message = new CompoundTag();
        message.putString("action", "return");
        container.handleMessage(message);
        helper.assertTrue(restored.casting().fluid().getAmount() == 90 && furnace(storage).getTank().getContained() == 0,
                "GUI return action must not cancel a valid cooling recipe after reload");
        tick(helper, restored, 40);
        helper.assertTrue(restored.canRemove() && restored.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT),
                "Finish only the remaining cooling time, then unlock removal");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void automaticPartialCastingResumesWhenFurnaceFluidChanges(GameTestHelper helper) {
        var storage = backpack(45);
        var casting = casting(storage, 1);
        casting.setAutomatic(true);
        tick(helper, casting, 100);
        helper.assertTrue(casting.casting().fluid().getAmount() == 45 && casting.casting().timer() == 0,
                "A half-filled automatic cast must wait without cooling");
        furnace(storage).getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 45), FluidAction.EXECUTE);
        tick(helper, casting, 60);
        helper.assertTrue(casting.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT)
                && furnace(storage).getTank().getContained() == 0 && casting.canRemove(), "Actual furnace fluid change resumes the existing half-filled recipe");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void addingAnotherUpgradeCannotRestoreAlreadyPouredFluid(GameTestHelper helper) {
        var storage = backpack(90);
        casting(storage, 1).pour(helper.getLevel());
        storage.getUpgradeHandler().setStackInSlot(2, new ItemStack(SophisticatedSmeltery.CASTING_UPGRADE.get()));
        var restored = casting(storage, 1);
        helper.assertTrue(furnace(storage).getTank().getContained() == 0 && restored.casting().fluid().getAmount() == 90,
                "Rebuilding real upgrade wrappers before the next furnace tick cannot restore already poured fluid");
        tick(helper, restored, 60);
        helper.assertTrue(restored.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT) && furnace(storage).getTank().getContained() == 0,
                "The saved casting batch finishes once and leaves no duplicate liquid");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void twoAutomaticUpgradesShareOneFurnaceWithoutDuplication(GameTestHelper helper) {
        var storage = backpack(180);
        storage.getUpgradeHandler().setStackInSlot(2, new ItemStack(SophisticatedSmeltery.CASTING_UPGRADE.get()));
        var first = casting(storage, 1);
        var second = casting(storage, 2);
        second.casting().inventory().setStackInSlot(0, first.casting().inventory().getStackInSlot(0).copy());
        first.setAutomatic(true);
        second.setAutomatic(true);
        for (int tick = 0; tick < 60; tick++) { tick(helper, first, 1); tick(helper, second, 1); }
        helper.assertTrue(first.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT)
                        && second.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT)
                        && furnace(storage).getTank().getContained() == 0, "Two independent upgrades consume and produce exactly two native recipes");
        helper.succeed();
    }
}
