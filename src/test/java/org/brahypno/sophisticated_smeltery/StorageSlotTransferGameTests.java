package org.brahypno.sophisticated_smeltery;

import java.util.Random;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import org.brahypno.sophisticated_smeltery.furnace.StorageSlotTransfer;

@PrefixGameTestTemplate(false)
public final class StorageSlotTransferGameTests {
    private static final int SLOTS = 144;

    private static final class CountingStorage extends BackpackWrapper {
        private final CountingInventory inventory;

        private CountingStorage() {
            super(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
            super.getInventoryHandler(); // Initialize the native contents UUID before creating upgrade handlers.
            inventory = new CountingInventory(this);
        }

        @Override public InventoryHandler getInventoryHandler() {
            return inventory == null ? super.getInventoryHandler() : inventory;
        }
    }

    private static final class CountingInventory extends InventoryHandler {
        private int insertions;
        private int reads;

        private CountingInventory(BackpackWrapper storage) {
            super(SLOTS, storage, new CompoundTag(), () -> {}, 64, Config.SERVER.stackUpgrade);
            setPersistent(false);
        }

        @Override protected boolean isAllowed(ItemStack stack) { return true; }
        @Override public ItemStack getStackInSlot(int slot) { reads++; return super.getStackInSlot(slot); }
        @Override public ItemStack insertItemOnlyToSlot(int slot, ItemStack stack, boolean simulate) {
            insertions++;
            return super.insertItemOnlyToSlot(slot, stack, simulate);
        }
        private void resetCounts() { insertions = 0; reads = 0; }
    }

    private static ItemStack referenceInsert(InventoryHandler inventory, ItemStack stack, int meltingSlots) {
        ItemStack remaining = stack;
        for (int pass = 0; pass < 2; pass++) {
            int start = pass == 0 ? meltingSlots : 0;
            int end = pass == 0 ? inventory.getSlots() : meltingSlots;
            for (int slot = start; slot < end && !remaining.isEmpty(); slot++)
                remaining = inventory.insertItemOnlyToSlot(slot, remaining, false);
        }
        return remaining;
    }

    private static ItemStack referenceExtract(InventoryHandler inventory, ItemStack target) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!ItemStack.isSameItemSameTags(target, inventory.getStackInSlot(slot))) continue;
            ItemStack taken = inventory.extractItem(slot, 1, false);
            if (!taken.isEmpty()) return taken;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack iron(int count, int variant) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT, count);
        if (variant != 0) stack.getOrCreateTag().putInt("variant", variant);
        return stack;
    }

    private static void sameContents(GameTestHelper helper, CountingStorage reference, CountingStorage indexed, String context) {
        for (int slot = 0; slot < SLOTS; slot++)
            helper.assertTrue(ItemStack.matches(reference.inventory.getStackInSlot(slot), indexed.inventory.getStackInSlot(slot)),
                    context + ": slot " + slot);
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void indexedTransfersMatchOrderedScan(GameTestHelper helper) {
        var reference = new CountingStorage();
        var indexed = new CountingStorage();
        var random = new Random(1729);
        for (int sample = 0; sample < 128; sample++) {
            for (int slot = 0; slot < SLOTS; slot++) {
                ItemStack stack = switch (random.nextInt(5)) {
                    case 0 -> ItemStack.EMPTY;
                    case 1 -> iron(64, random.nextInt(3));
                    case 2 -> iron(1 + random.nextInt(63), random.nextInt(3));
                    default -> new ItemStack(Items.COBBLESTONE, 64);
                };
                reference.inventory.setStackInSlot(slot, stack.copy());
                indexed.inventory.setStackInSlot(slot, stack.copy());
            }
            int blockedRemainder = sample % 7;
            reference.inventory.setSlotBlockedPredicate(slot -> slot % 7 == blockedRemainder, slot -> false);
            indexed.inventory.setSlotBlockedPredicate(slot -> slot % 7 == blockedRemainder, slot -> false);
            int prefix = sample == 0 ? 0 : sample == 1 ? SLOTS : random.nextInt(SLOTS + 1);
            ItemStack input = iron(1 + random.nextInt(192), random.nextInt(3));
            ItemStack expected = referenceInsert(reference.inventory, input.copy(), prefix);
            ItemStack actual = StorageSlotTransfer.insert(indexed, input.copy(), prefix);
            helper.assertTrue(ItemStack.matches(expected, actual), "Identical insertion remainder for sample " + sample);
            sameContents(helper, reference, indexed, "Insertion " + sample);
            ItemStack target = iron(1, random.nextInt(3));
            expected = referenceExtract(reference.inventory, target);
            actual = StorageSlotTransfer.extractOne(indexed.inventory, target);
            helper.assertTrue(ItemStack.matches(expected, actual), "Identical exact-NBT extraction for sample " + sample);
            sameContents(helper, reference, indexed, "Extraction " + sample);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void blockedTransfersSkipUnrelatedSlotsAndResumeImmediately(GameTestHelper helper) {
        var reference = new CountingStorage();
        var indexed = new CountingStorage();
        for (int slot = 0; slot < SLOTS; slot++) {
            reference.inventory.setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
            indexed.inventory.setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        reference.inventory.resetCounts();
        indexed.inventory.resetCounts();
        for (int tick = 0; tick < 100; tick++) {
            helper.assertTrue(referenceInsert(reference.inventory, iron(64, 0), 9).getCount() == 64, "Reference retains blocked output");
            helper.assertTrue(StorageSlotTransfer.insert(indexed, iron(64, 0), 9).getCount() == 64, "Indexed path retains blocked output");
        }
        helper.assertTrue(reference.inventory.insertions == 100 * SLOTS && indexed.inventory.insertions == 0,
                "Full unrelated inventory eliminates all 14,400 native insertion attempts");
        reference.inventory.resetCounts();
        indexed.inventory.resetCounts();
        helper.assertTrue(referenceExtract(reference.inventory, iron(1, 7)).isEmpty()
                && StorageSlotTransfer.extractOne(indexed.inventory, iron(1, 7)).isEmpty(), "Both wait for missing exact template");
        helper.assertTrue(reference.inventory.reads == SLOTS && indexed.inventory.reads == 0,
                "Missing template uses the index without reading 144 slots");
        for (int slot = 0; slot < SLOTS; slot++) {
            reference.inventory.setStackInSlot(slot, iron(64, 0));
            indexed.inventory.setStackInSlot(slot, iron(64, 0));
        }
        reference.inventory.resetCounts();
        indexed.inventory.resetCounts();
        helper.assertTrue(referenceInsert(reference.inventory, iron(64, 0), 9).getCount() == 64
                && StorageSlotTransfer.insert(indexed, iron(64, 0), 9).getCount() == 64,
                "Full matching stacks preserve the entire pending output");
        helper.assertTrue(reference.inventory.insertions == SLOTS && indexed.inventory.insertions == 0,
                "Skip full matching slots when no overflow upgrade can accept them");
        indexed.inventory.setStackInSlot(0, ItemStack.EMPTY);
        indexed.inventory.setStackInSlot(100, ItemStack.EMPTY);
        helper.assertTrue(StorageSlotTransfer.insert(indexed, iron(64, 0), 9).isEmpty()
                && indexed.inventory.getStackInSlot(100).getCount() == 64 && indexed.inventory.getStackInSlot(0).isEmpty(),
                "New ordinary space wins immediately over earlier melting space");
        indexed.inventory.setStackInSlot(143, iron(2, 7));
        helper.assertTrue(ItemStack.matches(StorageSlotTransfer.extractOne(indexed.inventory, iron(1, 7)), iron(1, 7))
                && indexed.inventory.getStackInSlot(143).getCount() == 1, "New exact template is extracted immediately and only once");
        SophisticatedSmeltery.LOGGER.info("SLOT TRANSFER ALGORITHM PASSED: 128 seeded layouts; blocked insertion attempts 14400 -> 0; missing-template slot reads 144 -> 0");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void nativeInsertCallbacksKeepTheirPerSlotBehavior(GameTestHelper helper) {
        for (boolean overflow : new boolean[]{false, true}) {
            var reference = new CountingStorage();
            var indexed = new CountingStorage();
            for (var storage : new CountingStorage[]{reference, indexed}) {
                storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.VOID_UPGRADE.get()));
                var wrapper = (VoidUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
                wrapper.setVoidType(overflow ? VoidType.SLOT_OVERFLOW : VoidType.ALWAYS);
                wrapper.getFilterLogic().setEmptyAllowListMatchesEverything();
                wrapper.setShouldWorkdInGUI(true);
                helper.assertTrue(!storage.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class).isEmpty(),
                        "Fixture has a native insertion callback");
                for (int slot = 0; slot < SLOTS; slot++)
                    storage.inventory.setStackInSlot(slot, new ItemStack(overflow ? Items.IRON_INGOT : Items.COBBLESTONE, 64));
                storage.inventory.resetCounts();
            }
            ItemStack expected = referenceInsert(reference.inventory, iron(64, 0), 9);
            ItemStack actual = StorageSlotTransfer.insert(indexed, iron(64, 0), 9);
            helper.assertTrue(expected.isEmpty() && ItemStack.matches(expected, actual)
                            && reference.inventory.insertions == indexed.inventory.insertions,
                    "Native void callbacks retain their result and exact per-slot invocation count");
            sameContents(helper, reference, indexed, "Native callbacks " + overflow);
        }
        helper.succeed();
    }
}
