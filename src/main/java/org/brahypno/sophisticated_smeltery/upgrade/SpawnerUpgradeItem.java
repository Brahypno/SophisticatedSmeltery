package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public final class SpawnerUpgradeItem extends UpgradeItemBase<SpawnerUpgradeWrapper> {
    public static final UpgradeType<SpawnerUpgradeWrapper> TYPE = new UpgradeType<>(SpawnerUpgradeWrapper::new);
    private static final IUpgradeCountLimitConfig LIMIT = new IUpgradeCountLimitConfig() {
        public int getMaxUpgradesPerStorage(String storage, ResourceLocation item) { return Integer.MAX_VALUE; }
        public int getMaxUpgradesInGroupPerStorage(String storage, UpgradeGroup group) { return Integer.MAX_VALUE; }
    };
    private final boolean advanced;
    public SpawnerUpgradeItem(boolean advanced) { super(LIMIT); this.advanced = advanced; }
    public boolean isAdvanced() { return advanced; }
    @Override public UpgradeType<SpawnerUpgradeWrapper> getType() { return TYPE; }
    @Override public int getMaxStackSize(ItemStack stack) { return stack.hasTag() ? 1 : 64; }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (advanced) tooltip.addAll(TranslationHelper.INSTANCE.getTranslatedLines(
                getDescriptionId().replace("advanced_", "") + ".tooltip", null, ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
    @Override public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        return List.of();
    }
}
