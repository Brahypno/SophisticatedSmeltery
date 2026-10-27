package org.brahypno.sophisticated_smeltery.mixin;

import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandlerSlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read the native indexes; their owner maintains them as contents and slot accessibility change. */
@Mixin(value = InventoryHandlerSlotTracker.class, remap = false)
public interface InventorySlotTrackerAccess {
    @Accessor("emptySlots") Set<Integer> sophisticatedsmeltery$getEmptySlots();
    @Accessor("itemStackKeys") Map<Item, Set<ItemStackKey>> sophisticatedsmeltery$getItemStackKeys();
    @Accessor("fullStackSlots") Map<ItemStackKey, Set<Integer>> sophisticatedsmeltery$getFullStackSlots();
    @Accessor("partiallyFilledStackSlots") Map<ItemStackKey, Set<Integer>> sophisticatedsmeltery$getPartialStackSlots();
}
