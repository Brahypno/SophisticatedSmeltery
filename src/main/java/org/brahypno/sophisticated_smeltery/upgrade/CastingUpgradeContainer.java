package org.brahypno.sophisticated_smeltery.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import org.brahypno.sophisticated_smeltery.casting.PortableCasting;

public final class CastingUpgradeContainer extends UpgradeContainerBase<CastingUpgradeWrapper, CastingUpgradeContainer> {
    public CastingUpgradeContainer(Player player, int id, CastingUpgradeWrapper wrapper,
                                   UpgradeContainerType<CastingUpgradeWrapper, CastingUpgradeContainer> type) {
        super(player, id, wrapper, type);
        slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.casting().inventory(), 0, -2000, -2000));
        slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.casting().inventory(), 1, -2000, -2000) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
    }
    public void action(String action) {
        sendDataToServer(() -> { CompoundTag data = new CompoundTag(); data.putString("action", action); return data; });
    }
    @Override public void handleMessage(CompoundTag data) {
        switch (data.getString("action")) {
            case "pour" -> upgradeWrapper.pour(player.level());
            case "return" -> upgradeWrapper.returnFluid(player.level());
            case "automatic" -> upgradeWrapper.setAutomatic(!upgradeWrapper.isAutomatic());
            case "enabled" -> upgradeWrapper.setCastingEnabled(!upgradeWrapper.isCastingEnabled());
            case "output" -> upgradeWrapper.setOutputToStorage(!upgradeWrapper.isOutputToStorage());
            case "lock" -> upgradeWrapper.casting().toggleCastLock();
            case "mode" -> upgradeWrapper.setMode(upgradeWrapper.casting().mode() == PortableCasting.Mode.TABLE
                    ? PortableCasting.Mode.BASIN : PortableCasting.Mode.TABLE);
            default -> { }
        }
    }
}
