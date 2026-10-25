package org.brahypno.sophisticated_smeltery;

import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeItem;
import org.brahypno.sophisticated_smeltery.client.CastingUpgradeSwitch;
import org.brahypno.sophisticated_smeltery.client.CastingUpgradeTab;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeContainer;
import org.lwjgl.glfw.GLFW;

/** Real screen clicks and server synchronization on a backpack with no melting upgrade. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class CastingExpansionClientTest {
    private static boolean running;
    private static int stage;
    private static int ticks;
    private static int frames;
    private static List<String> tooltipLines;
    private static int completedSlotHints;
    private static int pendingSlotHint = -1;

    static void start() {
        running = true;
        stage = -4;
        Minecraft.getInstance().getConnection().sendCommand("ssmeltery_test casting_expansion");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tooltip(RenderTooltipEvent.GatherComponents event) {
        if (running) tooltipLines = event.getTooltipElements().stream().flatMap(element -> element.left().stream()).map(line -> line.getString()).toList();
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (running && event.phase == TickEvent.Phase.END && ++ticks > 2400)
            throw new IllegalStateException("Casting expansion UI timed out at stage " + stage);
    }

    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if (!running || !(event.getScreen() instanceof StorageScreenBase<?> screen) || ++frames < 20) return;
        var mc = Minecraft.getInstance();
        if (stage == -3) {
            if (!screen.getMenu().getCarried().is(SophisticatedSmeltery.ADVANCED_SCORCHED_CASTING_UPGRADE.get())) return;
            check(screen.getMenu().upgradeSlots.get(1).mayPlace(screen.getMenu().getCarried()), "Real backpack slot must accept advanced scorched casting");
            clickSlot(screen, screen.getMenu().upgradeSlots.get(1));
            stage = -2;
            frames = 0;
            return;
        }
        if (!(screen.getMenu().getUpgradeContainers().get(1) instanceof CastingUpgradeContainer container)
                || !container.getUpgradeStack().is(SophisticatedSmeltery.ADVANCED_SCORCHED_CASTING_UPGRADE.get())) return;
        var casting = container.getUpgradeWrapper();
        var tab = tabs(screen).get(0);
        var storage = screen.getMenu().getStorageWrapper();
        var tank = storage.getUpgradeHandler().getTypeWrappers(TankUpgradeItem.TYPE).iterator().next();
        switch (stage) {
            case -4 -> clickSlot(screen, screen.getMenu().upgradeSlots.get(1));
            case -2 -> {
                if (!screen.getMenu().getCarried().isEmpty()) return;
                clickSlot(screen, screen.getMenu().upgradeSlots.get(3));
            }
            case -1 -> {
                if (!screen.getMenu().getCarried().is(SophisticatedSmeltery.SCORCHED_CASTING_UPGRADE.get())) return;
                check(screen.getMenu().upgradeSlots.get(3).mayPlace(screen.getMenu().getCarried()), "Real backpack slot must accept basic scorched casting");
                clickSlot(screen, screen.getMenu().upgradeSlots.get(3));
            }
            case 0 -> {
                if (!screen.getMenu().getCarried().isEmpty() || !screen.getMenu().upgradeSlots.get(3).hasItem()) return;
                for (var child : screen.getUpgradeSettingsControl().children()) {
                    if (child instanceof UpgradeSettingsTab<?> other && other.getWidth() > 21)
                        click(screen, other.getX() + 8, other.getY() + 12);
                }
                click(screen, tab.getX() + 8, tab.getY() + 12);
            }
            case 1 -> {
                if (!container.isOpen()) return;
                check(casting.controller().isEmpty() && casting.hasFluidStorage(), "Casting must operate with only a tank upgrade");
                check(casting.casting().requiresCast() && !casting.isAutomatic(), "Scorched tier defaults to manual");
                if (!verifySlotHint(event, screen, container, 0, 0, "cast_input", "casting-input-tooltip")) return;
                if (!verifySlotHint(event, screen, container, 1, 1, "cast_output", "casting-output-empty-tooltip")) return;
                capture(event, "casting-expansion-manual");
                TabFrameClientProbe.verify(event, tab);
                verifyLockIcon(event, tab, false, true);
                captureIcons(event);
                click(screen, tab.getX() + 37, tab.getY() + 63);
            }
            case 2 -> {
                if (casting.casting().lockedCast().isEmpty()) return;
                click(screen, tab.getX() + 109, tab.getY() + 101);
            }
            case 3 -> {
                if (!casting.isOutputToStorage()) return;
                click(screen, tab.getX() + 109, tab.getY() + 83);
            }
            case 4 -> {
                if (!casting.casting().isCooling()) return;
                var control = screen.children().stream().filter(CastingUpgradeSwitch.class::isInstance)
                        .map(CastingUpgradeSwitch.class::cast).filter(widget -> widget.getY() == screen.getGuiTop() + 24).findFirst().orElseThrow();
                check(control.getX() == screen.getGuiLeft() - 22, "Casting switch uses native position outside the inventory clipping region");
                click(screen, control.getX() + 3, control.getY() + 6);
            }
            case 5 -> {
                if (casting.isCastingEnabled() || casting.casting().hasFluid()) return;
                int ingots = count(storage.getInventoryHandler(), Items.IRON_INGOT);
                check(ingots == 1 && tank.getContents().getAmount() == 180, "Switch off finishes and delivers exactly one existing batch");
                check(casting.casting().inventory().getStackInSlot(0).isEmpty(), "Off does not start refilling the next cast");
                capture(event, "casting-expansion-off-output-storage");
                verifyLockIcon(event, tab, true, true);
                click(screen, screen.getGuiLeft() - 19, screen.getGuiTop() + 30);
            }
            case 6 -> {
                if (!casting.isCastingEnabled() || casting.casting().inventory().getStackInSlot(0).isEmpty()) return;
                click(screen, tab.getX() + 109, tab.getY() + 101);
            }
            case 7 -> {
                if (casting.isOutputToStorage()) return;
                click(screen, tab.getX() + 63, tab.getY() + 101);
            }
            case 8 -> {
                if (casting.casting().inventory().getStackInSlot(1).getCount() != 2 || casting.casting().hasFluid()) return;
                check(tank.getContents().isEmpty() && storage.getInventoryHandler().getStackInSlot(0).isEmpty(), "Locked sand casts refill from storage and consume exactly 270 mB");
                check(count(storage.getInventoryHandler(), Items.IRON_INGOT) == 1, "Output setting leaves two subsequent results stacked in the upgrade");
                if (!verifySlotHint(event, screen, container, 2, 0, "cast_input", "casting-input-empty-tooltip")) return;
                if (!verifySlotHint(event, screen, container, 3, 1, "cast_output", "casting-output-tooltip")) return;
                capture(event, "casting-expansion-stacked");
                click(screen, tab.getX() + 63, tab.getY() + 101);
                click(screen, tab.getX() + 37, tab.getY() + 63);
            }
            case 9 -> {
                if (casting.isAutomatic() || !casting.casting().lockedCast().isEmpty()) return;
                click(screen, tab.getX() + 8, tab.getY() + 12);
                var basic = tabs(screen).get(2);
                click(screen, basic.getX() + 8, basic.getY() + 12);
            }
            case 10 -> {
                var basicContainer = (CastingUpgradeContainer)screen.getMenu().getUpgradeContainers().get(3);
                if (!basicContainer.isOpen()) return;
                check(!basicContainer.getUpgradeWrapper().supportsAutomatic() && basicContainer.getUpgradeWrapper().casting().requiresCast(),
                        "Basic scorched upgrade retains the basic tier restriction");
                var basic = tabs(screen).get(2);
                TabFrameClientProbe.verify(event, basic);
                capture(event, "casting-expansion-basic");
                verifyLockIcon(event, basic, false, false);
                if (!mc.player.getRecipeBook().contains(SophisticatedSmeltery.id("scorched_casting_upgrade"))
                        || !mc.player.getRecipeBook().contains(SophisticatedSmeltery.id("advanced_scorched_casting_upgrade"))) return;
                check(CastingRecipeJeiProbe.visible(SophisticatedSmeltery.id("scorched_casting_upgrade"))
                        && CastingRecipeJeiProbe.visible(SophisticatedSmeltery.id("advanced_scorched_casting_upgrade")), "Scorched recipes appear in the real JEI crafting list");
                SophisticatedSmeltery.LOGGER.info("ACTUAL CASTING EXPANSION UI PASSED: normal installation of both scorched tiers, empty/filled input/output tooltips, standalone tank, switch off while cooling, storage/stacked outputs, locked consumed cast refill, four native icons and recipe book/JEI");
                running = false;
                mc.player.closeContainer();
                mc.stop();
            }
            default -> throw new IllegalStateException("Unexpected casting expansion stage " + stage);
        }
        SophisticatedSmeltery.LOGGER.info("Casting expansion UI stage {} completed", stage);
        stage++;
        frames = 0;
    }

    private static int count(net.minecraftforge.items.IItemHandler inventory, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) if (inventory.getStackInSlot(slot).is(item)) count += inventory.getStackInSlot(slot).getCount();
        return count;
    }
    private static List<CastingUpgradeTab> tabs(StorageScreenBase<?> screen) {
        return screen.getUpgradeSettingsControl().children().stream().filter(CastingUpgradeTab.class::isInstance).map(CastingUpgradeTab.class::cast).toList();
    }
    private static void click(StorageScreenBase<?> screen, int x, int y) {
        check(screen.mouseClicked(x, y, 0), "Actual screen must dispatch the click");
        screen.mouseReleased(x, y, 0);
    }
    private static void clickSlot(StorageScreenBase<?> screen, Slot slot) {
        click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
    }
    private static boolean verifySlotHint(ScreenEvent.Render.Post event, StorageScreenBase<?> screen,
                                          CastingUpgradeContainer container, int step, int index, String key, String filename) {
        if (step < completedSlotHints) return true;
        Slot slot = container.getSlots().get(index);
        if (pendingSlotHint != step) {
            pendingSlotHint = step;
            tooltipLines = List.of();
            movePointer(screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
            frames = 0;
            return false;
        }
        String role = Component.translatable("gui.sophisticated_smeltery." + key).getString();
        String expected = slot.hasItem() ? slot.getItem().getHoverName().getString() : role;
        check(!tooltipLines.isEmpty() && tooltipLines.get(0).equals(expected), "Slot tooltip must show the role only when empty, otherwise the original item: " + tooltipLines);
        check(!slot.hasItem() || !tooltipLines.contains(role), "A filled slot must not add an input/output label");
        capture(event, filename);
        completedSlotHints++;
        pendingSlotHint = -1;
        movePointer(4, 4);
        frames = 0;
        SophisticatedSmeltery.LOGGER.info("CASTING SLOT TOOLTIP PASSED: {}, filled={}", role, slot.hasItem());
        return false;
    }
    private static void movePointer(int x, int y) {
        var window = Minecraft.getInstance().getWindow();
        GLFW.glfwSetCursorPos(window.getWindow(), (double)x * window.getScreenWidth() / window.getGuiScaledWidth(),
                (double)y * window.getScreenHeight() / window.getGuiScaledHeight());
    }
    private static void capture(ScreenEvent.Render.Post event, String name) {
        event.getGuiGraphics().flush();
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) { image.writeToFile(Path.of("..", name + ".png")); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    private static void verifyLockIcon(ScreenEvent.Render.Post event, CastingUpgradeTab tab, boolean locked, boolean enabled) {
        var mc = Minecraft.getInstance();
        var reference = new LockIconButton(4, 110, button -> {});
        reference.setLocked(locked);
        reference.active = enabled;
        reference.render(event.getGuiGraphics(), -100, -100, 0);
        event.getGuiGraphics().flush();
        double scale = mc.getWindow().getGuiScale();
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            for (int dy = 0; dy < 20; dy++) for (int dx = 0; dx < 20; dx++) {
                int actual = image.getPixelRGBA((int)((tab.getX() + 27 + dx) * scale), (int)((tab.getY() + 53 + dy) * scale));
                int expected = image.getPixelRGBA((int)((4 + dx) * scale), (int)((110 + dy) * scale));
                check(actual == expected, "Cast lock icon must match the native locked/unlocked/disabled button at " + dx + "," + dy);
            }
        }
        SophisticatedSmeltery.LOGGER.info("CAST LOCK ICON MATCH: locked={}, enabled={}", locked, enabled);
    }
    private static void captureIcons(ScreenEvent.Render.Post event) {
        var mc = Minecraft.getInstance();
        var graphics = event.getGuiGraphics();
        ItemStack[] icons = {new ItemStack(SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get()), new ItemStack(SophisticatedSmeltery.ADVANCED_SEARED_CASTING_UPGRADE.get()),
                new ItemStack(SophisticatedSmeltery.SCORCHED_CASTING_UPGRADE.get()), new ItemStack(SophisticatedSmeltery.ADVANCED_SCORCHED_CASTING_UPGRADE.get())};
        graphics.fill(4, 16, 324, 102, 0xff19212d);
        for (int index = 0; index < icons.length; index++) {
            var model = mc.getItemRenderer().getModel(icons[index], mc.level, mc.player, 0);
            var quads = model.getQuads(null, null, RandomSource.create(0));
            final boolean scorched = index >= 2;
            check(quads.stream().anyMatch(quad -> quad.getSprite().contents().name().getPath().equals(scorched
                    ? "block/foundry/casting/table_plate" : "block/smeltery/casting/table_top")), "Use the matching native seared or scorched model");
            check(quads.stream().anyMatch(quad -> quad.getSprite().contents().name().getPath().endsWith("advanced_magnet_upgrade")) == (index % 2 == 1), "Advanced frame matches the tier");
            graphics.pose().pushPose();
            graphics.pose().translate(12 + index * 78, 28, 0);
            graphics.pose().scale(3, 3, 1);
            graphics.renderItem(icons[index], 0, 0);
            graphics.pose().popPose();
        }
        capture(event, "casting-expansion-icons");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
