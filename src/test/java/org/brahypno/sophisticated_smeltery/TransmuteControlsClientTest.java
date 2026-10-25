package org.brahypno.sophisticated_smeltery;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import org.brahypno.sophisticated_smeltery.client.MeltingUpgradeTab;
import org.brahypno.sophisticated_smeltery.client.BucketTransferModeButton;
import org.brahypno.sophisticated_smeltery.client.TransmuteControlButton;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;

/** Actual button dispatch and synchronized server state, with native icon screenshots. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class TransmuteControlsClientTest {
    private static boolean running;
    private static int stage;
    private static int ticks;
    private static int frames;
    static void start() { running = true; }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (running && event.phase == TickEvent.Phase.END && ++ticks > 2400)
            throw new IllegalStateException("Transmute controls UI timed out at stage " + stage);
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if (!running || !(event.getScreen() instanceof StorageScreenBase<?> screen) || ++frames < 20) return;
        var mc = Minecraft.getInstance();
        var container = (MeltingUpgradeContainer)screen.getMenu().getUpgradeContainers().get(0);
        var tab = screen.getUpgradeSettingsControl().children().stream().filter(MeltingUpgradeTab.class::isInstance)
                .map(MeltingUpgradeTab.class::cast).findFirst().orElseThrow();
        var wrapper = container.getUpgradeWrapper();
        if (stage == 0) {
            check(wrapper.hasTransmuteControls(), "Use the installed advanced Transmute upgrade");
            check(CastingRecipeJeiProbe.visible(SophisticatedSmeltery.id("advanced_transmute_upgrade")), "JEI must show the advanced recipe");
            check(mc.player.getRecipeBook().contains(SophisticatedSmeltery.id("advanced_transmute_upgrade")), "A standard bound upgrade unlocks the recipe book entry");
            for (var child : screen.getUpgradeSettingsControl().children()) {
                if (child instanceof UpgradeSettingsTab<?> other && other != tab && other.getWidth() > 21)
                    click(screen, other.getX() + 8, other.getY() + 12);
            }
            click(screen, tab.getX() + 8, tab.getY() + 12);
        } else {
            if (!container.isOpen()) return;
            var buttons = tab.children().stream().filter(TransmuteControlButton.class::isInstance)
                    .map(TransmuteControlButton.class::cast).toList();
            check(buttons.size() == 2, "Both advanced buttons must be visible");
            var mode = buttons.stream().filter(button -> !button.isAlloyButton()).findFirst().orElseThrow();
            var alloy = buttons.stream().filter(TransmuteControlButton::isAlloyButton).findFirst().orElseThrow();
            var bucket = tab.children().stream().filter(BucketTransferModeButton.class::isInstance)
                    .map(BucketTransferModeButton.class::cast).findFirst().orElseThrow();
            check(bucket.getY() + bucket.getHeight() + 4 <= mode.getY()
                    && alloy.getY() == mode.getY(), "Separate the container row from the two furnace controls below it");
            int gridBottom = screen.getGuiTop() + container.getSlots().get(8).y + 17;
            check(mode.getY() + mode.getHeight() == gridBottom && alloy.getY() + alloy.getHeight() == gridBottom,
                    "Align both advanced controls with the bottom edge of the component grid");
            int inputCenter = screen.getGuiLeft() + container.getSlots().get(9).x + 8;
            int bucketCenter = bucket.getX() + bucket.getWidth() / 2;
            int outputCenter = screen.getGuiLeft() + container.getSlots().get(10).x + 8;
            check(mode.getX() * 2 + mode.getWidth() == inputCenter + bucketCenter
                    && alloy.getX() * 2 + alloy.getWidth() == bucketCenter + outputCenter,
                    "Stagger the two advanced controls between the three icons above");
            switch (stage) {
                case 1, 2, 3 -> {
                    if (wrapper.getTransmuteMode() != stage - 1) return;
                    capture(event, "transmute-controls-mode-" + wrapper.getTransmuteMode());
                    click(screen, mode.getX() + 8, mode.getY() + 8);
                }
                case 4 -> {
                    if (wrapper.getTransmuteMode() != 0) return;
                    check(!wrapper.isTransmuteAlloying(), "Alloying defaults to off");
                    click(screen, alloy.getX() + 8, alloy.getY() + 8);
                }
                case 5 -> {
                    if (!wrapper.isTransmuteAlloying()) return;
                    capture(event, "transmute-controls-alloy-on");
                    click(screen, alloy.getX() + 8, alloy.getY() + 8);
                }
                case 6 -> {
                    if (wrapper.isTransmuteAlloying()) return;
                    capture(event, "transmute-controls-alloy-off");
                    SophisticatedSmeltery.LOGGER.info("TRANSMUTE CONTROLS UI PASSED: actual installed gold-frame upgrade, recipe book/JEI, three controller icons, native lever front on/off and authoritative button synchronization");
                    running = false;
                    mc.player.closeContainer(); mc.stop();
                }
            }
        }
        stage++; frames = 0;
    }
    private static void click(StorageScreenBase<?> screen, int x, int y) {
        check(screen.mouseClicked(x, y, 0), "Dispatch actual screen clicks");
        screen.mouseReleased(x, y, 0);
    }
    private static void capture(ScreenEvent.Render.Post event, String name) {
        event.getGuiGraphics().flush();
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            image.writeToFile(Path.of("..", name + ".png"));
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
