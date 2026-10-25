package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

/** Independent upgrade: does not participate in the melting-controller conflict group. */
public final class AlloyerUpgradeItem extends UpgradeItemBase<AlloyerUpgradeWrapper> {
    public static final UpgradeType<AlloyerUpgradeWrapper> TYPE = new UpgradeType<>(AlloyerUpgradeWrapper::new);
    private static final IUpgradeCountLimitConfig LIMIT = new IUpgradeCountLimitConfig() {
        public int getMaxUpgradesPerStorage(String storage, ResourceLocation item) { return 1; }
        public int getMaxUpgradesInGroupPerStorage(String storage, UpgradeGroup group) { return Integer.MAX_VALUE; }
    };
    public AlloyerUpgradeItem() { super(LIMIT); }
    @Override public UpgradeType<AlloyerUpgradeWrapper> getType() { return TYPE; }
    @Override public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() { return List.of(); }
    @Override public int getMaxStackSize(ItemStack stack) { return stack.hasTag() ? 1 : 64; }
    @Override public UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storage, boolean client) {
        return storage.getUpgradeHandler().getTypeWrappers(TYPE).stream().anyMatch(wrapper -> !wrapper.canRemove())
                ? new UpgradeSlotChangeResult.Fail(Component.translatable("error.sophisticated_smeltery.empty_alloyer"), Set.of(), Set.of(), Set.of())
                : new UpgradeSlotChangeResult.Success();
    }
    @Override public UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack target, int slot, IStorageWrapper storage, boolean client) {
        UpgradeSlotChangeResult removal = canRemoveUpgradeFrom(storage, client);
        return removal.isSuccessful() ? super.canSwapUpgradeFor(target, slot, storage, client) : removal;
    }
}
