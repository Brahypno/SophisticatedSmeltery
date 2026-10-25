package org.brahypno.sophisticated_smeltery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.client.event.ScreenEvent;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;

/** Compare real rendered panel pixels against the native magnet, including the 128-pixel boundary. */
final class TabFrameClientProbe {
    private static int[] nativeFrame;
    static void verifyContinuousHeader(ScreenEvent.Render.Post event, UpgradeSettingsTab<?> tab) {
        event.getGuiGraphics().flush();
        var mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        try (var screenshot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            for (int y = 0; y < 3; y++) {
                int expected = screenshot.getPixelRGBA((int)((tab.getX() + 8) * scale), (int)((tab.getY() + y) * scale));
                for (int x = 0; x < 6; x++) {
                    if (screenshot.getPixelRGBA((int)((tab.getX() + x) * scale), (int)((tab.getY() + y) * scale)) != expected)
                        throw new IllegalStateException("Melting header has a gap at " + x + "," + y);
                }
            }
        }
        SophisticatedSmeltery.LOGGER.info("MELTING HEADER PASSED: continuous top edge at width {}", tab.getWidth());
    }
    static void record(ScreenEvent.Render.Post event, UpgradeSettingsTab<?> magnet) { nativeFrame = pixels(event, magnet); }
    static void verify(ScreenEvent.Render.Post event, UpgradeSettingsTab<?> casting) {
        if (nativeFrame == null) throw new IllegalStateException("Native magnet frame reference was not captured");
        int[] actual = pixels(event, casting);
        if (!java.util.Arrays.equals(nativeFrame, actual))
            throw new IllegalStateException("Upgrade panel left border differs from native magnet at width " + casting.getWidth());
        SophisticatedSmeltery.LOGGER.info("TAB FRAME MATCH: {} pixels wide, native magnet left border matches", casting.getWidth());
    }
    private static int[] pixels(ScreenEvent.Render.Post event, UpgradeSettingsTab<?> tab) {
        event.getGuiGraphics().flush();
        var mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int[] result = new int[4 * 17];
        try (var screenshot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            int index = 0;
            // x..x+16 contains the icon. Include x itself, where the native 128px path draws a duplicate border.
            for (int y = 3; y < 20; y++) {
                for (int x = -3; x <= 0; x++)
                    result[index++] = screenshot.getPixelRGBA((int)((tab.getX() + x) * scale), (int)((tab.getY() + y) * scale));
            }
        }
        return result;
    }
}
