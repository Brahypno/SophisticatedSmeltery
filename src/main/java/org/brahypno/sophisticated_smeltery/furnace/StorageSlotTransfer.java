package org.brahypno.sophisticated_smeltery.furnace;

import java.util.BitSet;
import java.util.NavigableSet;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import org.brahypno.sophisticated_smeltery.mixin.InventorySlotTrackerAccess;

/** Preserve slot order and native transfer rules while skipping unrelated occupied slots. */
public final class StorageSlotTransfer {
    private static final BitSet NO_MATCHES = new BitSet();

    private StorageSlotTransfer() {}

    private static BitSet matchingSlots(InventorySlotTrackerAccess index, Item item, boolean includeFull) {
        var keys = index.sophisticatedsmeltery$getItemStackKeys().get(item);
        if (keys == null || keys.isEmpty()) return NO_MATCHES;
        BitSet slots = null;
        for (var key : keys) {
            var full = includeFull ? index.sophisticatedsmeltery$getFullStackSlots().get(key) : null;
            var partial = index.sophisticatedsmeltery$getPartialStackSlots().get(key);
            if (full == null && partial == null) continue;
            if (slots == null) slots = new BitSet();
            add(slots, full);
            add(slots, partial);
        }
        return slots == null ? NO_MATCHES : slots;
    }

    private static void add(BitSet slots, Set<Integer> matches) {
        if (matches != null) for (int slot : matches) slots.set(slot);
    }

    public static ItemStack extractOne(InventoryHandler inventory, ItemStack target) {
        var index = (InventorySlotTrackerAccess)inventory.getSlotTracker();
        BitSet matches = matchingSlots(index, target.getItem(), true);
        for (int slot = matches.nextSetBit(0); slot >= 0; slot = matches.nextSetBit(slot + 1)) {
            if (!ItemStack.isSameItemSameTags(target, inventory.getStackInSlot(slot))) continue;
            ItemStack taken = inventory.extractItem(slot, 1, false);
            if (!taken.isEmpty()) return taken;
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack insert(IStorageWrapper storage, ItemStack stack, int meltingSlots) {
        if (stack.isEmpty()) return stack;
        var inventory = storage.getInventoryHandler();
        // These callbacks may consume/transform input even in an unrelated occupied slot.
        // Keep their original per-slot invocation order when such an upgrade is installed.
        if (!storage.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class).isEmpty()) {
            ItemStack remaining = stack;
            for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
                int start = pass == 0 ? meltingSlots : 0;
                int end = pass == 0 ? inventory.getSlots() : meltingSlots;
                for (int slot = start; slot < end && !remaining.isEmpty(); slot++)
                    remaining = inventory.insertItemOnlyToSlot(slot, remaining, false);
            }
            return remaining;
        }
        var index = (InventorySlotTrackerAccess)inventory.getSlotTracker();
        boolean hasOverflow = !storage.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IOverflowResponseUpgrade.class).isEmpty();
        BitSet matches = matchingSlots(index, stack.getItem(), hasOverflow);
        // Sophisticated Core keeps this index as a TreeSet. Query it afresh after each
        // native transfer instead of retaining an iterator that the transfer invalidates.
        var empty = (NavigableSet<Integer>)index.sophisticatedsmeltery$getEmptySlots();
        ItemStack remaining = stack;
        for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
            int start = pass == 0 ? meltingSlots : 0;
            int end = pass == 0 ? inventory.getSlots() : meltingSlots;
            while (start < end) {
                int slot = matches.nextSetBit(start);
                Integer emptySlot = empty.ceiling(start);
                if (emptySlot != null && (slot < 0 || emptySlot < slot)) slot = emptySlot;
                if (slot < 0 || slot >= end) break;
                // Include full matching stacks: native overflow/void upgrades can accept them.
                remaining = inventory.insertItemOnlyToSlot(slot, remaining, false);
                start = slot + 1;
            }
        }
        return remaining;
    }
}
