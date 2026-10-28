package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import slimeknights.tconstruct.tables.menu.slot.ArmorSlot;

public final class TinkerStationUpgradeContainer extends UpgradeContainerBase<TinkerStationUpgradeWrapper, TinkerStationUpgradeContainer> {
    private final Slot output;
    private final List<Slot> equipmentSlots = new ArrayList<>();
    public TinkerStationUpgradeContainer(Player player, int id, TinkerStationUpgradeWrapper wrapper,
            UpgradeContainerType<TinkerStationUpgradeWrapper, TinkerStationUpgradeContainer> type) {
        super(player, id, wrapper, type);
        wrapper.bind(player.level());
        for (int slot = 0; slot < wrapper.inventory().getSlots(); slot++)
            slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.inventory(), slot, -2000, -2000));
        output = new ToolResultSlot(); slots.add(output);
        for (ArmorItem.Type typeOfArmor : ArmorItem.Type.values()) {
            equipmentSlots.add(new ArmorSlot(player.getInventory(), typeOfArmor.getSlot(), -2000, -2000) {
                @Override public boolean mayPickup(Player player) { return super.mayPickup(player) && !upgradeWrapper.isHostStack(getItem()); }
                @Override public boolean isActive() { return isOpen(); }
            });
        }
        equipmentSlots.add(new Slot(player.getInventory(), 40, -2000, -2000) {
            @Override public boolean mayPickup(Player player) { return !upgradeWrapper.isHostStack(getItem()); }
            @Override public boolean mayPlace(ItemStack stack) { return !upgradeWrapper.isHostStack(getItem()); }
            @Override public boolean isActive() { return isOpen(); }
        }.setBackground(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));
        slots.addAll(equipmentSlots);
    }
    public Slot resultSlot() { return output; }
    public List<Slot> equipmentSlots() { return List.copyOf(equipmentSlots); }
    public void select(ResourceLocation name) {
        upgradeWrapper.select(name);
        sendDataToServer(() -> { var tag = new CompoundTag(); tag.putString("layout", name.toString()); return tag; });
    }
    public void rename(String name) {
        upgradeWrapper.rename(name);
        sendDataToServer(() -> { var tag = new CompoundTag(); tag.putString("name", name); return tag; });
    }
    public void toggleDestination() {
        boolean value = !upgradeWrapper.shiftIntoStorage();
        upgradeWrapper.setShiftIntoStorage(value); sendBooleanToServer("destination", value);
    }
    @Override public void handleMessage(CompoundTag data) {
        if (player.isSpectator()) return;
        upgradeWrapper.bind(player.level());
        if (data.contains("layout")) {
            var name = ResourceLocation.tryParse(data.getString("layout"));
            if (name != null) upgradeWrapper.select(name);
        }
        if (data.contains("name")) upgradeWrapper.rename(data.getString("name"));
        if (data.contains("destination")) upgradeWrapper.setShiftIntoStorage(data.getBoolean("destination"));
    }
    @Override public boolean allowsPickupAll(Slot slot) { return slot != output; }
    @Override public boolean mergeIntoStorageFirst(Slot slot) { return slot != output || upgradeWrapper.shiftIntoStorage(); }
    @Override public int getRepeatedQuickMoveLimit(Slot slot, ItemStack stack) {
        return slot == output ? Math.max(1, stack.getMaxStackSize() / stack.getCount()) : 0;
    }
    private final class ToolResultSlot extends Slot {
        private TinkerStationUpgradeWrapper displayedWrapper;
        private int displayedRevision = -1;
        private ItemStack prepared = ItemStack.EMPTY;
        private ToolResultSlot() { super(new SimpleContainer(1), 0, -2000, -2000); }
        @Override public ItemStack getItem() {
            upgradeWrapper.bind(player.level());
            int revision = upgradeWrapper.revision();
            if (displayedWrapper != upgradeWrapper || displayedRevision != revision) {
                displayedWrapper = upgradeWrapper; displayedRevision = revision;
                prepared = upgradeWrapper.result(); container.setItem(0, prepared.copy());
            }
            return container.getItem(0);
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public ItemStack remove(int amount) { getItem(); return container.removeItemNoUpdate(0); }
        @Override public void onTake(Player player, ItemStack stack) {
            ItemStack remainder = container.removeItemNoUpdate(0);
            upgradeWrapper.craft(player, prepared.copy());
            if (!player.level().isClientSide) ItemHandlerHelper.giveItemToPlayer(player, remainder);
            displayedRevision = -1;
        }
    }
}
