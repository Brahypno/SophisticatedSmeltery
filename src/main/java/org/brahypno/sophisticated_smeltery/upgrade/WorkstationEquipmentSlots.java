package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.tables.menu.slot.ArmorSlot;

/** Player-owned equipment aliases shared by the portable tool workstations. */
final class WorkstationEquipmentSlots {
    private WorkstationEquipmentSlots() { }
    static List<Slot> create(Player player, BooleanSupplier open, Predicate<ItemStack> hostStack) {
        var slots = new ArrayList<Slot>();
        for (ArmorItem.Type armor : ArmorItem.Type.values()) {
            slots.add(new ArmorSlot(player.getInventory(), armor.getSlot(), -2000, -2000) {
                @Override public boolean mayPickup(Player player) { return super.mayPickup(player) && !hostStack.test(getItem()); }
                @Override public boolean isActive() { return open.getAsBoolean(); }
            });
        }
        slots.add(new Slot(player.getInventory(), 40, -2000, -2000) {
            @Override public boolean mayPickup(Player player) { return !hostStack.test(getItem()); }
            @Override public boolean mayPlace(ItemStack stack) { return !hostStack.test(getItem()); }
            @Override public boolean isActive() { return open.getAsBoolean(); }
        }.setBackground(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));
        return List.copyOf(slots);
    }
}
