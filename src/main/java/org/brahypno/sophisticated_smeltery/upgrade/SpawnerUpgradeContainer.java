package org.brahypno.sophisticated_smeltery.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

public final class SpawnerUpgradeContainer extends UpgradeContainerBase<SpawnerUpgradeWrapper, SpawnerUpgradeContainer> {
    public SpawnerUpgradeContainer(Player player, int id, SpawnerUpgradeWrapper wrapper,
                                  UpgradeContainerType<SpawnerUpgradeWrapper, SpawnerUpgradeContainer> type) {
        super(player, id, wrapper, type);
        slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.inventory(), 0, -2000, -2000));
    }
    @Override public void handleMessage(CompoundTag data) { }
}
