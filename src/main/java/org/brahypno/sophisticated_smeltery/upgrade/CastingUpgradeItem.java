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

public final class CastingUpgradeItem extends UpgradeItemBase<CastingUpgradeWrapper> {
    private final boolean advanced;
    private final boolean scorched;
    public static final UpgradeType<CastingUpgradeWrapper> TYPE = new UpgradeType<>(CastingUpgradeWrapper::new);
    private static final IUpgradeCountLimitConfig UNLIMITED = new IUpgradeCountLimitConfig() {
        public int getMaxUpgradesPerStorage(String storage, ResourceLocation item) { return Integer.MAX_VALUE; }
        public int getMaxUpgradesInGroupPerStorage(String storage, UpgradeGroup group) { return Integer.MAX_VALUE; }
    };
    public CastingUpgradeItem() { this(false); }
    public CastingUpgradeItem(boolean advanced) { this(advanced, false); }
    public CastingUpgradeItem(boolean advanced, boolean scorched) {
        super(UNLIMITED); this.advanced = advanced; this.scorched = scorched;
    }
    public boolean isAdvanced() { return advanced; }
    public boolean isScorched() { return scorched; }
    @Override public UpgradeType<CastingUpgradeWrapper> getType() { return TYPE; }
    @Override public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() { return List.of(); }
    @Override public int getMaxStackSize(ItemStack stack) { return stack.hasTag() ? 1 : 64; }

    @Override public UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack target, int slot, IStorageWrapper storage, boolean client) {
        if (storage.getUpgradeHandler().getSlotWrappers().get(slot) instanceof CastingUpgradeWrapper casting && !casting.canRemove())
            return new UpgradeSlotChangeResult.Fail(Component.translatable("error.sophisticatedsmeltery.empty_casting"), Set.of(slot), Set.of(), Set.of());
        return super.canSwapUpgradeFor(target, slot, storage, client);
    }
}
