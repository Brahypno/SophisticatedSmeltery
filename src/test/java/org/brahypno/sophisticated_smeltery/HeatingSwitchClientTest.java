package org.brahypno.sophisticated_smeltery;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ItemButton;
import org.brahypno.sophisticated_smeltery.client.MeltingHeatingSwitch;
import org.brahypno.sophisticated_smeltery.client.MeltingUpgradeTab;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import org.lwjgl.glfw.GLFW;

/** Actual screen, scroll clipping, input dispatch, server packet and resize regression. Opt-in only. */
@Mod.EventBusSubscriber(modid = "sophisticatedsmeltery", value = Dist.CLIENT)
public final class HeatingSwitchClientTest {
    private static int stage;
    private static int ticks;
    private static int frames;
    private static int size;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.heatingSwitchSmoke") || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (stage >= 9) return;
        if (++ticks > 7200) throw new IllegalStateException("Actual backpack UI test timed out at stage " + stage
                + ", screen=" + (mc.screen == null ? "null" : mc.screen.getClass().getName()));
        if (stage == 0 && mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) {
            mc.setScreen(new TitleScreen());
        }
        if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            mc.options.guiScale().set(2);
            resize(mc, 1280, 480);
            stage = 1;
            mc.createWorldOpenFlows().createFreshLevel("switch-test-" + System.currentTimeMillis(),
                    new LevelSettings("Switch UI test", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42L, false, false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        } else if (stage == 1 && mc.player != null && mc.getConnection() != null && mc.screen == null) {
            mc.getConnection().sendCommand("ssmeltery_test");
            stage = 3;
            ticks = 0;
        }
    }

    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.heatingSwitchSmoke") || stage < 3 || stage >= 9
                || !(event.getScreen() instanceof StorageScreenBase<?> screen) || ++frames < 20) return;
        var mc = Minecraft.getInstance();
        var control = screen.children().stream().filter(MeltingHeatingSwitch.class::isInstance)
                .map(MeltingHeatingSwitch.class::cast).findFirst().orElseThrow();
        var container = screen.getMenu().getUpgradeContainers().values().stream().filter(MeltingUpgradeContainer.class::isInstance)
                .map(MeltingUpgradeContainer.class::cast).findFirst().orElseThrow();
        var tabs = screen.getUpgradeSettingsControl().children().stream().filter(UpgradeSettingsTab.class::isInstance)
                .map(child -> (UpgradeSettingsTab<?>)child).toList();
        boolean scrolling = screen.children().stream().anyMatch(child -> child.getClass().getSimpleName().equals("InventoryScrollPanel"));
        if (stage == 3) {
            check(scrolling == (size == 0), "Exercise both actual scrolling and non-scrolling backpack screens");
            check(control.getX() == screen.getGuiLeft() - 22 && control.getY() == screen.getGuiTop() + 8,
                    "Heating switch must use the native upgrade switch position");
            check(!container.getUpgradeWrapper().isHeating(), "Fixture must start with heating off");
            for (var tab : tabs) assertIconAnchor(tab);
            capture(event, "switch-" + size + "-closed");
            click(screen, control.getX() + 3, control.getY() + 6);
            stage = 4;
        } else if (stage == 4 && container.getUpgradeWrapper().isHeating()) {
            capture(event, "switch-" + size + "-on");
            click(screen, control.getX() + 3, control.getY() + 6);
            stage = 5;
        } else if (stage == 5 && !container.getUpgradeWrapper().isHeating()) {
            var melting = tabs.stream().filter(MeltingUpgradeTab.class::isInstance).findFirst().orElseThrow();
            click(screen, melting.getX() + 8, melting.getY() + 12);
            stage = 6;
        } else if (stage == 6 && container.isOpen()) {
            var melting = tabs.stream().filter(MeltingUpgradeTab.class::isInstance).findFirst().orElseThrow();
            assertIconAnchor(melting);
            capture(event, "switch-" + size + "-melting-open");
            // The open panel covers the following tab. Close it before clicking the magnet.
            click(screen, melting.getX() + 8, melting.getY() + 12);
            stage = 7;
        } else if (stage == 7 && !container.isOpen()) {
            var magnet = tabs.stream().filter(tab -> !(tab instanceof MeltingUpgradeTab)).findFirst().orElseThrow();
            click(screen, magnet.getX() + 8, magnet.getY() + 12);
            stage = 8;
        } else if (stage == 8 && tabs.stream().anyMatch(tab -> !(tab instanceof MeltingUpgradeTab) && tab.getWidth() > 21)) {
            for (var tab : tabs) assertIconAnchor(tab);
            capture(event, "switch-" + size + "-magnet-open");
            if (size++ == 0) {
                // Close the native magnet tab before resizing and reinitializing the screen.
                var magnet = tabs.stream().filter(tab -> !(tab instanceof MeltingUpgradeTab)).findFirst().orElseThrow();
                click(screen, magnet.getX() + 8, magnet.getY() + 12);
                resize(mc, 1280, 900);
                stage = 3;
            } else {
                SophisticatedSmeltery.LOGGER.info("ACTUAL BACKPACK UI PASSED: scroll/no-scroll, off/on/off server synchronization, native icon anchors and open-tab captures");
                if (Boolean.getBoolean("sophisticatedsmeltery.castingUpgradeSmoke")) CastingUpgradeClientTest.start();
                else if (Boolean.getBoolean("sophisticatedsmeltery.meltingInteractionSmoke")) MeltingInteractionClientTest.start();
                else { mc.player.closeContainer(); mc.stop(); }
                stage = 9;
            }
        } else return;
        frames = 0;
    }

    private static void assertIconAnchor(UpgradeSettingsTab<?> tab) {
        var icon = tab.children().stream().filter(ItemButton.class::isInstance).map(ItemButton.class::cast).findFirst().orElseThrow();
        check(icon.getX() - tab.getX() == 1 && icon.getY() - tab.getY() == 4,
                "Closed and open icons must retain the native magnet anchor");
    }

    private static void resize(Minecraft mc, int width, int height) {
        long window = mc.getWindow().getWindow();
        GLFW.glfwRestoreWindow(window);
        GLFW.glfwShowWindow(window);
        GLFW.glfwSetWindowSize(window, width, height);
        GLFW.glfwFocusWindow(window);
    }

    private static void click(StorageScreenBase<?> screen, int x, int y) {
        check(screen.mouseClicked(x, y, 0), "Screen must dispatch the actual click");
        screen.mouseReleased(x, y, 0);
    }

    private static void capture(ScreenEvent.Render.Post event, String name) {
        event.getGuiGraphics().flush();
        var target = Minecraft.getInstance().getMainRenderTarget();
        check(target.width >= 1280 && target.height >= 480, "Screenshot must have a valid framebuffer");
        try (var image = Screenshot.takeScreenshot(target)) { image.writeToFile(Path.of("..", name + ".png")); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
