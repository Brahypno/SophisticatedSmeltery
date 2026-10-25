package org.brahypno.sophisticated_smeltery.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeItem;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistration {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            UpgradeGuiManager.registerTab(SophisticatedSmeltery.MELTING_CONTAINER, MeltingUpgradeTab::new);
            UpgradeGuiManager.registerTab(SophisticatedSmeltery.CASTING_CONTAINER, CastingUpgradeTab::new);
            UpgradeGuiManager.registerTab(SophisticatedSmeltery.ALLOYER_CONTAINER, AlloyerUpgradeTab::new);
            UpgradeGuiManager.registerInventoryControl(MeltingUpgradeItem.TYPE, screen ->
                    screen.getMenu().getUpgradeContainers().values().stream()
                            .filter(MeltingUpgradeContainer.class::isInstance)
                            .map(MeltingUpgradeContainer.class::cast).findFirst()
                            .map(container -> new MeltingSlotOverlay(screen, container)));
        });
    }
}
