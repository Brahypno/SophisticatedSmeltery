package org.brahypno.sophisticated_smeltery;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;

/** Captures the first actual open melting tab during a manual dev run. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class ManualGuiCapture {
    private static int frames;
    private static boolean captured;
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.manualCapture") || captured
                || !(event.getScreen() instanceof StorageScreenBase<?> screen)) return;
        boolean meltingOpen = screen.getMenu().getUpgradeContainers().values().stream()
                .anyMatch(container -> container instanceof MeltingUpgradeContainer && container.isOpen());
        if (!meltingOpen) { frames = 0; return; }
        if (++frames < 15) return;
        var target = Minecraft.getInstance().getMainRenderTarget();
        if (target.width < 128 || target.height < 128) return;
        event.getGuiGraphics().flush();
        try (var image = Screenshot.takeScreenshot(target)) {
            image.writeToFile(Path.of("../build/manual-melting-screen.png"));
            captured = true;
            SophisticatedSmeltery.LOGGER.info("Actual melting tab captured to build/manual-melting-screen.png");
        } catch (Exception exception) { throw new IllegalStateException("Cannot capture manual melting tab", exception); }
    }
}
