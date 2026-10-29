package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public final class ModifierWorktableUpgradeItem extends UpgradeItemBase<ModifierWorktableUpgradeWrapper> {
    public static final UpgradeType<ModifierWorktableUpgradeWrapper> TYPE = new UpgradeType<>(ModifierWorktableUpgradeWrapper::new);
    private static final IUpgradeCountLimitConfig LIMIT = new IUpgradeCountLimitConfig() {
        public int getMaxUpgradesPerStorage(String storage, ResourceLocation item) { return 1; }
        public int getMaxUpgradesInGroupPerStorage(String storage, UpgradeGroup group) { return Integer.MAX_VALUE; }
    };
    public ModifierWorktableUpgradeItem() { super(LIMIT); }
    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
            List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        ItemStack source = TinkerStationAppearance.source(stack);
        source.getItem().appendHoverText(source, level, tooltip, flag);
    }
    @Override public UpgradeType<ModifierWorktableUpgradeWrapper> getType() { return TYPE; }
    @Override public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() { return List.of(); }
    @Override public int getMaxStackSize(ItemStack stack) { return stack.hasTag() ? 1 : 64; }
}
