package org.brahypno.sophisticated_smeltery;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
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
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherInventoryControl;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import org.brahypno.sophisticated_smeltery.client.SpawnerUpgradeTab;
import org.brahypno.sophisticated_smeltery.mixin.StorageScreenControlsAccess;
import org.brahypno.sophisticated_smeltery.upgrade.SpawnerUpgradeContainer;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class SpawnerUpgradeClientTest {
    private static int stage, ticks, frames, captured;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.spawnerSmoke") || event.phase != TickEvent.Phase.END || stage >= 10) return;
        var mc = Minecraft.getInstance();
        if (++ticks > 6000) throw new IllegalStateException("Spawner UI timed out at " + stage);
        if (stage == 0 && mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) mc.setScreen(new TitleScreen());
        if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            mc.options.guiScale().set(2);
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
            stage = 1;
            mc.createWorldOpenFlows().createFreshLevel("spawner-test-" + System.currentTimeMillis(),
                    new LevelSettings("Spawner UI test", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42L, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        } else if (stage == 1 && mc.player != null && mc.screen == null && mc.getConnection() != null) {
            mc.getConnection().sendCommand("ssmeltery_test spawner");
            stage = 2;
        }
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.spawnerSmoke") || stage < 2 || stage >= 10
                || !(event.getScreen() instanceof StorageScreenBase<?> screen) || ++frames < 20) return;
        var mc = Minecraft.getInstance();
        var container = screen.getMenu().getUpgradeContainers().values().stream().filter(SpawnerUpgradeContainer.class::isInstance)
                .map(SpawnerUpgradeContainer.class::cast).findFirst().orElseThrow();
        var wrapper = container.getUpgradeWrapper();
        var tab = screen.getUpgradeSettingsControl().children().stream().filter(SpawnerUpgradeTab.class::isInstance)
                .map(SpawnerUpgradeTab.class::cast).findFirst().orElseThrow();
        var storage = (IBackpackWrapper)screen.getMenu().getStorageWrapper();
        int count = MobCatcherStorage.getCapturedMobs(storage).size();
        switch (stage) {
            case 2 -> { click(screen, tab.getX() + 8, tab.getY() + 12); }
            case 3 -> {
                if (!container.isOpen()) return;
                check(container.getSlots().size() == 1 && screen.getMenu().canDisableUpgrade(0) && !wrapper.isEnabled(), "One spawner slot and native module switch");
                var slot = screen.getMenu().slots.stream().filter(value -> value.container == mc.player.getInventory() && value.getSlotIndex() == 11).findFirst().orElseThrow();
                click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
            }
            case 4 -> {
                if (!screen.getMenu().getCarried().is(Items.SPAWNER)) return;
                var slot = container.getSlots().get(0);
                click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
            }
            case 5 -> {
                if (wrapper.inventory().getStackInSlot(0).isEmpty() || !screen.getMenu().getCarried().isEmpty()) return;
                click(screen, screen.getGuiLeft() - 19, screen.getGuiTop() + 11);
            }
            case 6 -> {
                if (!wrapper.isEnabled() || count < 1) return;
                check(((StorageScreenControlsAccess)screen).sophisticatedsmeltery$getStorageControls().get(MobCatcherUpgradeItem.TYPE) instanceof MobCatcherInventoryControl,
                        "Actual catcher control displays the newborn mob");
                capture("spawner-ui-running");
                click(screen, screen.getGuiLeft() - 19, screen.getGuiTop() + 11);
            }
            case 7 -> {
                if (wrapper.isEnabled()) return;
                captured = count;
                frames = -60;
            }
            case 8 -> {
                check(!wrapper.isEnabled() && count == captured, "Module off pauses births while preserving captured mobs");
                capture("spawner-ui-off");
                var slot = container.getSlots().get(0);
                click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
            }
            case 9 -> {
                if (!wrapper.inventory().getStackInSlot(0).isEmpty() || !screen.getMenu().getCarried().is(Items.SPAWNER)) return;
                check(net.minecraft.world.item.BlockItem.getBlockEntityData(screen.getMenu().getCarried()).getShort("Delay") > 0, "Removing the real spawner preserves paid cooldown");
                SophisticatedSmeltery.LOGGER.info("SPAWNER UI PASSED: real slot insertion/removal, native module switch, server spawning/capture, client display, disabled pause and saved spawner NBT");
                stage = 10;
                mc.player.closeContainer(); mc.stop();
                return;
            }
            default -> throw new IllegalStateException("Unexpected stage " + stage);
        }
        stage++; frames = stage == 8 ? -60 : 0;
    }
    private static void click(StorageScreenBase<?> screen, int x, int y) { check(screen.mouseClicked(x, y, 0), "Actual screen interaction"); screen.mouseReleased(x, y, 0); }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static void capture(String name) {
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) { image.writeToFile(Path.of("..", name + ".png")); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
}
