package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

public final class ModifierWorktableUpgradeContainer extends UpgradeContainerBase<ModifierWorktableUpgradeWrapper, ModifierWorktableUpgradeContainer> {
    private final Slot output;
    private final List<Slot> equipmentSlots;
    public ModifierWorktableUpgradeContainer(Player player, int id, ModifierWorktableUpgradeWrapper wrapper,
            UpgradeContainerType<ModifierWorktableUpgradeWrapper, ModifierWorktableUpgradeContainer> type) {
        super(player, id, wrapper, type);
        wrapper.bind(player.level());
        for (int slot = 0; slot < wrapper.inventory().getSlots(); slot++)
            slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.inventory(), slot, -2000, -2000));
        output = new WorktableResultSlot(); slots.add(output);
        equipmentSlots = WorkstationEquipmentSlots.create(player, this::isOpen, stack -> upgradeWrapper.isHostStack(stack));
        slots.addAll(equipmentSlots);
    }
    public Slot resultSlot() { return output; }
    public List<Slot> equipmentSlots() { return equipmentSlots; }
    public void select(int index) {
        upgradeWrapper.select(index);
        sendDataToServer(() -> { var data = new CompoundTag(); data.putInt("modifier", index); return data; });
    }
    public void toggleDestination() {
        boolean value = !upgradeWrapper.shiftIntoStorage();
        upgradeWrapper.setShiftIntoStorage(value); sendBooleanToServer("destination", value);
    }
    @Override public void handleMessage(CompoundTag data) {
        if (player.isSpectator()) return;
        upgradeWrapper.bind(player.level());
        if (data.contains("modifier")) upgradeWrapper.select(data.getInt("modifier"));
        if (data.contains("destination")) upgradeWrapper.setShiftIntoStorage(data.getBoolean("destination"));
    }
    @Override public boolean allowsPickupAll(Slot slot) { return slot != output; }
    @Override public boolean mergeIntoStorageFirst(Slot slot) { return slot != output || upgradeWrapper.shiftIntoStorage(); }
    @Override public int getRepeatedQuickMoveLimit(Slot slot, ItemStack stack) { return 0; }

    private final class WorktableResultSlot extends Slot {
        private ModifierWorktableUpgradeWrapper displayedWrapper;
        private int displayedRevision = -1;
        private ItemStack prepared = ItemStack.EMPTY;
        private WorktableResultSlot() { super(new SimpleContainer(1), 0, -2000, -2000); }
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
