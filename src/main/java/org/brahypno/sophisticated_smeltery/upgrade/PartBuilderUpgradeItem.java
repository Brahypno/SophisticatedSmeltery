package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public final class PartBuilderUpgradeItem extends UpgradeItemBase<PartBuilderUpgradeWrapper> {
    public static final UpgradeType<PartBuilderUpgradeWrapper> TYPE = new UpgradeType<>(PartBuilderUpgradeWrapper::new);
    private static final IUpgradeCountLimitConfig LIMIT = new IUpgradeCountLimitConfig() {
        public int getMaxUpgradesPerStorage(String storage, ResourceLocation item) { return 1; }
        public int getMaxUpgradesInGroupPerStorage(String storage, UpgradeGroup group) { return Integer.MAX_VALUE; }
    };
    public PartBuilderUpgradeItem() { super(LIMIT); }
    @Override public UpgradeType<PartBuilderUpgradeWrapper> getType() { return TYPE; }
    @Override public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() { return List.of(); }
    @Override public int getMaxStackSize(ItemStack stack) { return stack.hasTag() ? 1 : 64; }
}
