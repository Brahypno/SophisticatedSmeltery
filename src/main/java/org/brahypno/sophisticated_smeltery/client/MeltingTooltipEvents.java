package org.brahypno.sophisticated_smeltery.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class MeltingTooltipEvents {
    @SubscribeEvent public static void itemTooltip(RenderTooltipEvent.Pre event) {
        if (event.getItemStack().isEmpty() || !(Minecraft.getInstance().screen instanceof StorageScreenBase<?> screen)) return;
        // Vanilla slot hover includes a 1-pixel border. Suppress that item tooltip
        // only over our heat bar; its separate status tooltip has no ItemStack.
        for (var container : screen.getMenu().getUpgradeContainers().values()) {
            if (container instanceof MeltingUpgradeContainer melting
                    && MeltingSlotOverlay.isHeatBarHovered(screen, melting, event.getX(), event.getY())) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
