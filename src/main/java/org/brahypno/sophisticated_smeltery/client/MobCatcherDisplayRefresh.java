package org.brahypno.sophisticated_smeltery.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherInventoryControl;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.mixin.StorageScreenControlsAccess;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;

/** Upgrade contents can arrive after the screen's first attempt to create its native controls. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class MobCatcherDisplayRefresh {
    private MobCatcherDisplayRefresh() {}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(Minecraft.getInstance().screen instanceof StorageScreenBase<?> screen)
                || !screen.getMenu().isUpgradeColumnCountSynced()
                || screen.getMenu().getUpgradeContainers().values().stream().noneMatch(MeltingUpgradeContainer.class::isInstance)) return;
        var controls = ((StorageScreenControlsAccess)screen).sophisticatedsmeltery$getStorageControls();
        if (!controls.containsKey(MobCatcherUpgradeItem.TYPE)) {
            MobCatcherInventoryControl.create(screen).ifPresent(control -> controls.put(MobCatcherUpgradeItem.TYPE, control));
        }
    }
}
