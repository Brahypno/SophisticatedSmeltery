package org.brahypno.sophisticated_smeltery.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

public final class PartBuilderUpgradeContainer extends UpgradeContainerBase<PartBuilderUpgradeWrapper, PartBuilderUpgradeContainer> {
    private final Slot output;

    public PartBuilderUpgradeContainer(Player player, int id, PartBuilderUpgradeWrapper wrapper,
            UpgradeContainerType<PartBuilderUpgradeWrapper, PartBuilderUpgradeContainer> type) {
        super(player, id, wrapper, type);
        wrapper.bind(player.level());
        slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.inventory(), PartBuilderUpgradeWrapper.PATTERN, -2000, -2000));
        slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.inventory(), PartBuilderUpgradeWrapper.MATERIAL, -2000, -2000));
        output = new PartResultSlot();
        slots.add(output);
    }
    public void select(Pattern pattern) {
        upgradeWrapper.select(pattern);
        sendDataToServer(() -> { var data = new CompoundTag(); data.putString("pattern", pattern.toString()); return data; });
    }
    public void toggleDestination() {
        boolean value = !upgradeWrapper.shiftIntoStorage();
        upgradeWrapper.setShiftIntoStorage(value);
        sendBooleanToServer("destination", value);
    }
    @Override public void handleMessage(CompoundTag data) {
        if (player.isSpectator()) return;
        upgradeWrapper.bind(player.level());
        if (data.contains("pattern")) {
            Pattern pattern = Pattern.tryParse(data.getString("pattern"));
            if (pattern != null) upgradeWrapper.select(pattern);
        }
        if (data.contains("destination")) upgradeWrapper.setShiftIntoStorage(data.getBoolean("destination"));
    }
    @Override public boolean allowsPickupAll(Slot slot) { return slot != output; }
    @Override public int getRepeatedQuickMoveLimit(Slot slot, ItemStack stack) {
        return slot == output ? Math.max(1, stack.getMaxStackSize() / stack.getCount()) : 0;
    }
    @Override public boolean mergeIntoStorageFirst(Slot slot) { return slot != output || upgradeWrapper.shiftIntoStorage(); }

    /** A complete recipe is taken at once, like Tinkers' LazyResultSlot. */
    private final class PartResultSlot extends Slot {
        private PartBuilderUpgradeWrapper displayedWrapper;
        private int displayedRevision = -1;
        private ItemStack prepared = ItemStack.EMPTY;
        private PartResultSlot() { super(new SimpleContainer(1), 0, -2000, -2000); }
        @Override public ItemStack getItem() {
            upgradeWrapper.bind(player.level());
            int revision = upgradeWrapper.revision();
            if (displayedWrapper != upgradeWrapper || displayedRevision != revision) {
                displayedWrapper = upgradeWrapper;
                displayedRevision = revision;
                prepared = upgradeWrapper.result();
                container.setItem(0, prepared.copy());
            }
            return container.getItem(0);
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public ItemStack remove(int amount) {
            getItem();
            return container.removeItemNoUpdate(0);
        }
        @Override public void onTake(Player player, ItemStack stack) {
            ItemStack remainder = container.removeItemNoUpdate(0);
            upgradeWrapper.craft(player, prepared.copy());
            if (!player.level().isClientSide) ItemHandlerHelper.giveItemToPlayer(player, remainder);
            displayedRevision = -1;
        }
    }
}
