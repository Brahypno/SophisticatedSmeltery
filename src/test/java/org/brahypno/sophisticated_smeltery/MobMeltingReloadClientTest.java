package org.brahypno.sophisticated_smeltery;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherInventoryControl;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import org.brahypno.sophisticated_smeltery.client.MeltingHeatingSwitch;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;

/** Actual save, disconnect, disk reload, item use and captured-mob display regression. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class MobMeltingReloadClientTest {
    private static int stage = -1;
    private static int ticks;
    private static int frames;
    private static int health;
    private static String world;
    private static UUID mobId;

    public static void start(int stoppedHealth) {
        var mc = Minecraft.getInstance();
        world = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString();
        health = stoppedHealth;
        mobId = mob((StorageScreenBase<?>)mc.screen).id();
        ticks = 0;
        clickHeating((StorageScreenBase<?>)mc.screen);
        stage = 0;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (stage < 0 || event.phase != TickEvent.Phase.END) return;
        check(++ticks < 7200, "Mob reload test timed out at stage " + stage);
        var mc = Minecraft.getInstance();
        if (stage == 0 && mc.screen instanceof StorageScreenBase<?> screen && container(screen).getUpgradeWrapper().isHeating()) {
            health = mob(screen).currentHealth();
            mc.player.closeContainer();
            mc.level.disconnect();
            mc.clearLevel(new TitleScreen());
            stage = 1;
        } else if (stage == 1 && mc.screen instanceof TitleScreen && mc.getSingleplayerServer() == null && mc.getOverlay() == null) {
            mc.createWorldOpenFlows().loadLevel(new TitleScreen(), world);
            stage = 2;
        } else if (stage == 2 && mc.player != null && mc.level != null && mc.screen == null) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            stage = 3;
            frames = 0;
        } else if (stage == 5 && ++frames > 60 && mc.screen instanceof StorageScreenBase<?> screen) {
            var container = container(screen);
            check(!container.getUpgradeWrapper().isHeating() && mob(screen).currentHealth() == health,
                    "After actual reload, turning heating off must stop damage");
            assertDisplayed(screen, mob(screen));
            SophisticatedSmeltery.LOGGER.info("ACTUAL MOB RELOAD UI PASSED: saved with heating on, disk reload, visible same captured mob, resumed damage, shared switch and stable health={}", health);
            stage = -1;
            mc.player.closeContainer();
            mc.stop();
        }
    }

    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if ((stage != 3 && stage != 4) || !(event.getScreen() instanceof StorageScreenBase<?> screen) || ++frames < 30) return;
        var mob = mob(screen);
        assertDisplayed(screen, mob);
        if (stage == 3) {
            check(mob.id().equals(mobId) && mob.currentHealth() > 0 && mob.currentHealth() <= health,
                    "Actual save/rejoin preserves the same captured mob while heating continues");
            check(container(screen).getUpgradeWrapper().isHeating(), "The active heating state must survive disk reload");
            health = mob.currentHealth();
            capture("mob-reload-loaded");
            stage = 4;
        } else if (mob.currentHealth() < health) {
            health = mob.currentHealth();
            capture("mob-reload-heating");
            clickHeating(screen);
            stage = 5;
            frames = 0;
        }
    }

    static void assertDisplayed(StorageScreenBase<?> screen, CapturedMob mob) {
        var controls = (Map<?, ?>)field(StorageScreenBase.class, screen, "storageInventoryControls");
        var control = controls.values().stream().filter(MobCatcherInventoryControl.class::isInstance)
                .map(MobCatcherInventoryControl.class::cast).findFirst().orElseThrow(() ->
                        new IllegalStateException("The actual screen is missing its captured-mob display control"));
        var viewed = (IBackpackWrapper)field(MobCatcherInventoryControl.class, control, "backpackWrapper");
        check(control.replacesSlotRender(mob.slot()), "Captured mob exists in current menu but its display control has stale contents: menu="
                + mob.id() + ", display=" + MobCatcherStorage.getCapturedMobs(viewed));
        var renderEntities = (Map<?, ?>)field(MobCatcherInventoryControl.class, control, "capturedMobRenderEntities");
        check(renderEntities.containsKey(mob.id()), "Captured mob must have a model in the actual display control");
    }

    private static Object field(Class<?> owner, Object target, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }

    private static MeltingUpgradeContainer container(StorageScreenBase<?> screen) {
        return screen.getMenu().getUpgradeContainers().values().stream().filter(MeltingUpgradeContainer.class::isInstance)
                .map(MeltingUpgradeContainer.class::cast).findFirst().orElseThrow();
    }

    private static CapturedMob mob(StorageScreenBase<?> screen) {
        var mobs = MobCatcherStorage.getCapturedMobs((IBackpackWrapper)container(screen).getUpgradeWrapper().getStorage());
        check(mobs.size() == 1, "Reloaded actual backpack must contain its captured mob");
        return mobs.get(0);
    }

    private static void clickHeating(StorageScreenBase<?> screen) {
        var button = screen.children().stream().filter(MeltingHeatingSwitch.class::isInstance).map(MeltingHeatingSwitch.class::cast).findFirst().orElseThrow();
        check(screen.mouseClicked(button.getX() + 3, button.getY() + 6, 0), "Actual heating switch click");
        screen.mouseReleased(button.getX() + 3, button.getY() + 6, 0);
    }

    private static void capture(String name) {
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            image.writeToFile(Path.of("..", name + ".png"));
        } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }

    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
