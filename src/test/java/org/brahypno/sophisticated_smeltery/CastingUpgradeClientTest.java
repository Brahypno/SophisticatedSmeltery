package org.brahypno.sophisticated_smeltery;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import org.brahypno.sophisticated_smeltery.casting.PortableCasting;
import org.brahypno.sophisticated_smeltery.client.CastingUpgradeTab;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import slimeknights.tconstruct.fluids.TinkerFluids;

/** Opt-in actual screen and authoritative server state test, following the heating switch regression. */
@Mod.EventBusSubscriber(modid = "sophisticatedsmeltery", value = Dist.CLIENT)
public final class CastingUpgradeClientTest {
    private static boolean running;
    private static int stage;
    private static int ticks;
    private static int frames;
    static void start() { running = true; stage = -2; }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (running && event.phase == TickEvent.Phase.END && ++ticks > 2400)
            throw new IllegalStateException("Actual casting GUI timed out at stage " + stage);
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if (!running || !(event.getScreen() instanceof StorageScreenBase<?> screen) || ++frames < 20) return;
        var mc = Minecraft.getInstance();
        var container = screen.getMenu().getUpgradeContainers().get(2) instanceof CastingUpgradeContainer casting ? casting : null;
        var tab = screen.getUpgradeSettingsControl().children().stream().filter(CastingUpgradeTab.class::isInstance)
                .map(CastingUpgradeTab.class::cast).findFirst().orElse(null);
        if (stage < 21 && (container == null || tab == null)) return;
        var wrapper = container == null ? null : container.getUpgradeWrapper();
        var carried = screen.getMenu().getCarried();
        var furnace = screen.getMenu().getUpgradeContainers().values().stream().filter(MeltingUpgradeContainer.class::isInstance)
                .map(MeltingUpgradeContainer.class::cast).findFirst().orElseThrow().getUpgradeWrapper();
        switch (stage) {
            case -2 -> {
                for (var child : screen.getUpgradeSettingsControl().children()) {
                    if (child instanceof UpgradeSettingsTab<?> other && other.getWidth() > 21)
                        click(screen, other.getX() + 8, other.getY() + 12);
                }
                var basicTab = screen.getUpgradeSettingsControl().children().stream().filter(CastingUpgradeTab.class::isInstance)
                        .map(CastingUpgradeTab.class::cast).reduce((first, second) -> second).orElseThrow();
                click(screen, basicTab.getX() + 8, basicTab.getY() + 12);
            }
            case -1 -> {
                var basic = (CastingUpgradeContainer)screen.getMenu().getUpgradeContainers().get(3);
                if (!basic.isOpen()) return;
                var basicTab = screen.getUpgradeSettingsControl().children().stream().filter(CastingUpgradeTab.class::isInstance)
                        .map(CastingUpgradeTab.class::cast).reduce((first, second) -> second).orElseThrow();
                check(!basic.getUpgradeWrapper().supportsAutomatic(), "Basic tier must not expose automatic casting");
                check(basicTab.children().stream().noneMatch(child -> child instanceof net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase widget
                        && widget.getX() == basicTab.getX() + 45 && widget.getY() == basicTab.getY() + 92), "Basic GUI must have no automatic toggle");
                capture(event, "casting-upgrade-basic");
                TabFrameClientProbe.verify(event, basicTab);
                captureTierIcons(event);
                click(screen, basicTab.getX() + 8, basicTab.getY() + 12);
            }
            case 0 -> {
                for (var child : screen.getUpgradeSettingsControl().children()) {
                    if (child instanceof UpgradeSettingsTab<?> other && other != tab && other.getWidth() > 21)
                        click(screen, other.getX() + 8, other.getY() + 12);
                }
                click(screen, tab.getX() + 8, tab.getY() + 12);
            }
            case 1 -> {
                if (!container.isOpen()) return;
                check(!wrapper.isAutomatic() && !furnace.isHeating(), "Begin with manual casting and furnace heating OFF");
                capture(event, "casting-upgrade-manual");
                TabFrameClientProbe.verify(event, tab);
                click(screen, tab.getX() + 23, tab.getY() + 101);
            }
            case 2 -> {
                if (!wrapper.casting().isCooling()) return;
                capture(event, "casting-upgrade-cooling");
                clickSlot(screen, screen.getMenu().upgradeSlots.get(2));
                mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId,
                        screen.getMenu().upgradeSlots.get(2).index, 0, ClickType.QUICK_MOVE, mc.player);
            }
            case 3 -> {
                if (!wrapper.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT)) return;
                check(carried.isEmpty() && screen.getMenu().upgradeSlots.get(2).hasItem(), "Normal and shift removal must both keep the cooling upgrade installed");
                clickSlot(screen, container.getSlots().get(1));
            }
            case 4 -> { if (!carried.is(Items.IRON_INGOT)) return; clickSlot(screen, playerSlot(screen, 5)); }
            case 5 -> { if (!carried.isEmpty()) return; click(screen, tab.getX() + 80, tab.getY() + 101); }
            case 6 -> {
                if (!wrapper.isAutomatic() || !wrapper.casting().isCooling()) return;
                click(screen, tab.getX() + 80, tab.getY() + 101);
            }
            case 7 -> {
                if (wrapper.isAutomatic() || !wrapper.casting().inventory().getStackInSlot(1).is(Items.IRON_INGOT)) return;
                clickSlot(screen, container.getSlots().get(1));
            }
            case 8 -> { if (!carried.is(Items.IRON_INGOT)) return; clickSlot(screen, playerSlot(screen, 5)); }
            case 9 -> { if (!carried.isEmpty()) return; clickSlot(screen, container.getSlots().get(0)); }
            case 10 -> { if (carried.isEmpty()) return; clickSlot(screen, playerSlot(screen, 6)); }
            case 11 -> { if (!carried.isEmpty()) return; click(screen, tab.getX() + 17, tab.getY() + 63); }
            case 12 -> {
                if (wrapper.casting().mode() != PortableCasting.Mode.BASIN) return;
                click(screen, tab.getX() + 23, tab.getY() + 101);
            }
            case 13 -> {
                if (!wrapper.casting().isCooling()) return;
                capture(event, "casting-upgrade-basin");
            }
            case 14 -> {
                if (!wrapper.casting().inventory().getStackInSlot(1).is(Items.IRON_BLOCK)) return;
                clickSlot(screen, container.getSlots().get(1));
            }
            case 15 -> { if (!carried.is(Items.IRON_BLOCK)) return; clickSlot(screen, playerSlot(screen, 7)); }
            case 16 -> { if (!carried.isEmpty()) return; click(screen, tab.getX() + 23, tab.getY() + 101); }
            case 17 -> {
                if (wrapper.casting().fluid().getAmount() != 10 || wrapper.casting().isCooling()) return;
                capture(event, "casting-upgrade-partial");
                clickSlot(screen, screen.getMenu().upgradeSlots.get(2));
                check(screen.getMenu().getCarried().isEmpty(), "Partial fluid must also block normal removal");
                click(screen, tab.getX() + 60, tab.getY() + 122);
            }
            case 18 -> {
                if (wrapper.casting().hasFluid()) return;
                clickSlot(screen, screen.getMenu().upgradeSlots.get(3));
            }
            case 19 -> { if (!carried.is(SophisticatedSmeltery.CASTING_UPGRADE.get())) return; clickSlot(screen, playerSlot(screen, 8)); }
            case 20 -> { if (!carried.isEmpty()) return; clickSlot(screen, screen.getMenu().upgradeSlots.get(2)); }
            case 21 -> { if (!carried.is(SophisticatedSmeltery.ADVANCED_CASTING_UPGRADE.get())) return; clickSlot(screen, playerSlot(screen, 9)); }
            case 22 -> {
                if (!carried.isEmpty()) return;
                if (!mc.player.getRecipeBook().contains(SophisticatedSmeltery.id("casting_upgrade"))
                        || !mc.player.getRecipeBook().contains(SophisticatedSmeltery.id("advanced_casting_upgrade"))) return;
                check(CastingRecipeJeiProbe.visible(SophisticatedSmeltery.id("casting_upgrade"))
                                && CastingRecipeJeiProbe.visible(SophisticatedSmeltery.id("advanced_casting_upgrade")),
                        "Both tiers must be in JEI's actual visible crafting list");
                SophisticatedSmeltery.LOGGER.info("CASTING RECIPES VISIBLE: both tiers unlocked in the actual client recipe book and listed in JEI");
                check(playerSlot(screen, 5).getItem().is(Items.IRON_INGOT) && playerSlot(screen, 5).getItem().getCount() == 2
                                && playerSlot(screen, 7).getItem().is(Items.IRON_BLOCK), "Keep exactly two ingots and one iron block");
                check(furnace.getTank().getContained() == 1010, "Preserve 1000 mB gold and the 10 mB returned iron remainder");
                int iron = furnace.getTank().getFluids().stream().filter(fluid -> fluid.getFluid() == TinkerFluids.moltenIron.get()).mapToInt(fluid -> fluid.getAmount()).sum();
                check(iron == 10 && !furnace.isHeating(), "1000 mB initial iron equals 180 mB ingots + 810 mB block + 10 mB returned, with heat OFF throughout");
                check(!screen.getMenu().upgradeSlots.get(2).hasItem() && !screen.getMenu().upgradeSlots.get(3).hasItem(), "Both clean casting upgrades are normally removable");
                capture(event, "casting-upgrade-complete");
                SophisticatedSmeltery.LOGGER.info("ACTUAL CASTING UI PASSED: manual/automatic, heat OFF, native table/basin outputs, cooldown, normal/shift removal locks, partial return and exact conservation");
                running = false;
                mc.player.closeContainer();
                mc.stop();
            }
            default -> throw new IllegalStateException("Unexpected casting GUI stage " + stage);
        }
        SophisticatedSmeltery.LOGGER.info("Casting UI stage {} completed", stage);
        stage++;
        frames = 0;
    }
    private static Slot playerSlot(StorageScreenBase<?> screen, int index) {
        return screen.getMenu().slots.stream().filter(slot -> slot.container == Minecraft.getInstance().player.getInventory()
                && slot.getSlotIndex() == index).findFirst().orElseThrow();
    }
    private static void clickSlot(StorageScreenBase<?> screen, Slot slot) { click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8); }
    private static void click(StorageScreenBase<?> screen, int x, int y) {
        check(screen.mouseClicked(x, y, 0), "Actual screen must dispatch casting click");
        screen.mouseReleased(x, y, 0);
    }
    private static void capture(ScreenEvent.Render.Post event, String name) {
        event.getGuiGraphics().flush();
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) { image.writeToFile(Path.of("..", name + ".png")); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    private static void captureTierIcons(ScreenEvent.Render.Post event) {
        var mc = Minecraft.getInstance();
        var graphics = event.getGuiGraphics();
        graphics.fill(4, 16, 164, 102, 0xff19212d);
        ItemStack[] icons = {new ItemStack(SophisticatedSmeltery.CASTING_UPGRADE.get()), new ItemStack(SophisticatedSmeltery.ADVANCED_CASTING_UPGRADE.get())};
        for (int index = 0; index < icons.length; index++) {
            var model = mc.getItemRenderer().getModel(icons[index], mc.level, mc.player, 0);
            var quads = model.getQuads(null, null, RandomSource.create(0));
            check(quads.stream().anyMatch(quad -> quad.getSprite().contents().name().getPath().endsWith("casting/table_top")),
                    "Both icons must actually use the native casting-table top model");
            check(quads.stream().anyMatch(quad -> quad.getSprite().contents().name().getPath().endsWith("advanced_magnet_upgrade")) == (index == 1),
                    "Only the advanced icon may use the native gold frame");
            graphics.pose().pushPose();
            graphics.pose().translate(12 + index * 78, 28, 0);
            graphics.pose().scale(3, 3, 1);
            graphics.renderItem(icons[index], 0, 0);
            graphics.pose().popPose();
            graphics.drawString(mc.font, index == 0 ? "Basic" : "Advanced", 13 + index * 78, 84, 0xffffffff, false);
        }
        capture(event, "casting-tier-icons");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
