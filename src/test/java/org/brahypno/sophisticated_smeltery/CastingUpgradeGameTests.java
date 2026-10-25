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
        return backpack(amount, false);
    }
    private static BackpackWrapper backpack(int amount, boolean advanced) {
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()),
                new ItemStack(TinkerSmeltery.smelteryController)));
        var furnace = furnace(storage);
        furnace.getComponents().setStackInSlot(0, new ItemStack(TinkerSmeltery.searedBricks, 32));
        furnace.setHeating(false);
        furnace.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), amount), FluidAction.EXECUTE);
        furnace.persist();
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(advanced
                ? SophisticatedSmeltery.ADVANCED_SEARED_CASTING_UPGRADE.get() : SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get()));
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
    public static void basicTierStaysManualAndCastsWithHeatingOff(GameTestHelper helper) {
        var storage = backpack(180);
        for (var item : java.util.List.of(SophisticatedSmeltery.SEARED_CASTING_UPGRADE, SophisticatedSmeltery.ADVANCED_SEARED_CASTING_UPGRADE,
                SophisticatedSmeltery.SCORCHED_CASTING_UPGRADE, SophisticatedSmeltery.ADVANCED_SCORCHED_CASTING_UPGRADE)) {
            ItemStack upgrade = new ItemStack(item.get());
            helper.assertTrue(upgrade.is(ModItems.BACKPACK_UPGRADE_TAG), "Normal backpack slots require the upgrade tag: " + item.getId());
            helper.assertTrue(storage.getUpgradeHandler().insertItem(3, upgrade, true).isEmpty()
                    && storage.getUpgradeHandler().insertItem(3, upgrade, false).isEmpty(), "Install through the normal handler: " + item.getId());
            helper.assertTrue(!storage.getUpgradeHandler().extractItem(3, 1, false).isEmpty(), "An idle installed upgrade remains removable");
        }
        var casting = casting(storage, 1);
        tick(helper, casting, 10);
        helper.assertTrue(!casting.isAutomatic() && furnace(storage).getTank().getContained() == 180,
                "A new upgrade defaults to manual and cannot pour just because it is installed");
        var container = new CastingUpgradeContainer(helper.makeMockPlayer(), 1, casting, SophisticatedSmeltery.CASTING_CONTAINER);
        CompoundTag message = new CompoundTag();
        casting.getUpgradeStack().getOrCreateTag().putBoolean("automatic", true);
        var loaded = new CastingUpgradeWrapper(storage, casting.getUpgradeStack().copy(), ignored -> {});
        helper.assertTrue(!loaded.supportsAutomatic() && !loaded.isAutomatic(), "Old automatic NBT cannot enable the basic tier");
        message.putString("action", "automatic");
        container.handleMessage(message);
        tick(helper, casting, 100);
        helper.assertTrue(!casting.isAutomatic() && furnace(storage).getTank().getContained() == 180,
                "A basic upgrade rejects the advanced GUI action without consuming fluid");
        message.putString("action", "pour");
        container.handleMessage(message);
        helper.assertTrue(!furnace(storage).isHeating() && casting.casting().isCooling() && furnace(storage).getTank().getContained() == 90,
                "Manual GUI message uses existing furnace fluid despite heating being off");
        tick(helper, casting, 60);
        helper.assertTrue(casting.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT), "Cooling completes while heating stays off");
        helper.assertTrue(storage.getInventoryHandler().getStackInSlot(0).isEmpty(), "Independent output must not enter a melting input");
        loaded = new CastingUpgradeWrapper(storage, casting.getUpgradeStack().copy(), ignored -> {});
        helper.assertTrue(loaded.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT) && loaded.canRemove(),
                "Persist finished output and allow removing the clean upgrade with its items");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void automaticCastingRemovalReloadAndSharedFluidLifecycle(GameTestHelper helper) {
        var storage = backpack(45, true);
        storage.getUpgradeHandler().setStackInSlot(2, new ItemStack(SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get()));
        var busy = casting(storage, 1);
        helper.assertTrue(SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get().canAddUpgradeTo(storage,
                new ItemStack(SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get()), true, false).isSuccessful(), "Multiple casting upgrades have no added count limit");
        busy.setAutomatic(true);
        tick(helper, busy, 100);
        helper.assertTrue(busy.casting().fluid().getAmount() == 45 && busy.casting().timer() == 0, "Automatic half batch waits for more fluid");
        helper.assertTrue(!busy.canRemove() && !busy.casting().isCooling(), "Partial fluid locks removal even before cooling starts");
        helper.assertTrue(storage.getUpgradeHandler().extractItem(1, 1, true).isEmpty()
                && storage.getUpgradeHandler().extractItem(1, 1, false).isEmpty(), "Both simulated and actual extraction must keep a filled upgrade installed");
        helper.assertTrue(!SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get().canSwapUpgradeFor(
                new ItemStack(SophisticatedSmeltery.ADVANCED_SEARED_CASTING_UPGRADE.get()), 1, storage, false).isSuccessful(), "A filled upgrade cannot be bypassed by same-item swap");
        helper.assertTrue(!storage.getUpgradeHandler().extractItem(2, 1, false).isEmpty(), "Another empty casting upgrade remains independently removable");
        busy = casting(storage, 1); // The native upgrade handler rebuilds wrappers after another slot is removed.
        helper.assertTrue(busy.returnFluid(helper.getLevel()) == 45 && busy.canRemove(), "Return partial fluid before removal");
        ItemStack clean = storage.getUpgradeHandler().extractItem(1, 1, false);
        helper.assertTrue(!clean.isEmpty(), "The same upgrade unlocks after fluid is returned");
        storage.getUpgradeHandler().setStackInSlot(1, clean);
        var casting = casting(storage, 1);
        tick(helper, casting, 10);
        helper.assertTrue(casting.casting().fluid().getAmount() == 45 && !casting.casting().isCooling(),
                "Reinstalled automatic caster waits on the same half batch");
        furnace(storage).getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 45), FluidAction.EXECUTE);
        tick(helper, casting, 20);
        storage.getUpgradeHandler().setStackInSlot(2, new ItemStack(SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get()));
        var restored = casting(storage, 1);
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

        // With the first paid output still present, rebuild a second automatic caster.
        furnace(storage).getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 180), FluidAction.EXECUTE);
        restored.pour(helper.getLevel());
        storage.getUpgradeHandler().setStackInSlot(2, new ItemStack(SophisticatedSmeltery.ADVANCED_SEARED_CASTING_UPGRADE.get()));
        var first = casting(storage, 1);
        var second = casting(storage, 2);
        helper.assertTrue(furnace(storage).getTank().getContained() == 90 && first.casting().fluid().getAmount() == 90,
                "Rebuilding before furnace tick cannot restore already poured fluid");
        second.casting().inventory().setStackInSlot(0, first.casting().inventory().getStackInSlot(0).copy());
        first.setAutomatic(true);
        second.setAutomatic(true);
        for (int tick = 0; tick < 60; tick++) { tick(helper, first, 1); tick(helper, second, 1); }
        helper.assertTrue(first.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT)
                && second.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT)
                && first.casting().inventory().getStackInSlot(1).getCount() == 2
                && second.casting().inventory().getStackInSlot(1).getCount() == 1
                && furnace(storage).getTank().getContained() == 0 && first.canRemove() && second.canRemove(),
                "Two independent casters stack and consume exactly two additional recipes");
        helper.succeed();
    }
}
