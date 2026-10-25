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
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import org.brahypno.sophisticated_smeltery.client.AlloyerUpgradeTab;
import org.brahypno.sophisticated_smeltery.client.AlloyerUpgradeSwitch;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import slimeknights.tconstruct.fluids.TinkerFluids;

/** Actual GUI dispatch and server synchronization for the independent alloyer. Opt-in only. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class AlloyerUpgradeClientTest {
    private static int stage, ticks, frames;
    private static int stoppedFuel;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.alloyerSmoke") || event.phase != TickEvent.Phase.END || stage >= 12) return;
        var mc = Minecraft.getInstance();
        if (++ticks > 7200) throw new IllegalStateException("Alloyer client test timed out at stage " + stage);
        if (stage == 0 && mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) mc.setScreen(new TitleScreen());
        if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            mc.options.guiScale().set(2);
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
            stage = 1;
            mc.createWorldOpenFlows().createFreshLevel("alloyer-test-" + System.currentTimeMillis(),
                    new LevelSettings("Alloyer UI test", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42L, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        } else if (stage == 1 && mc.player != null && mc.screen == null && mc.getConnection() != null) {
            mc.getConnection().sendCommand("ssmeltery_test alloyer");
            stage = 2;
        }
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.alloyerSmoke") || stage < 2 || stage >= 12
                || !(event.getScreen() instanceof StorageScreenBase<?> screen) || ++frames < 20) return;
        var container = screen.getMenu().getUpgradeContainers().values().stream().filter(AlloyerUpgradeContainer.class::isInstance)
                .map(AlloyerUpgradeContainer.class::cast).findFirst().orElseThrow();
        var tab = screen.getUpgradeSettingsControl().children().stream().filter(AlloyerUpgradeTab.class::isInstance)
                .map(AlloyerUpgradeTab.class::cast).findFirst().orElseThrow();
        var wrapper = container.getUpgradeWrapper();
        var mc = Minecraft.getInstance();
        var carried = screen.getMenu().getCarried();
        switch (stage) {
            case 2 -> {
                for (var child : screen.getUpgradeSettingsControl().children())
                    if (child instanceof UpgradeSettingsTab<?> other && other != tab && other.getWidth() > 21) click(screen, other.getX() + 8, other.getY() + 12, 0);
                click(screen, tab.getX() + 8, tab.getY() + 12, 0);
            }
            case 3 -> {
                if (!container.isOpen()) return;
                check(tab.children().stream().noneMatch(net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton.class::isInstance),
                        "The settings tab has no second heating switch");
                check(container.getSlots().size() == 11 && wrapper.getTank().getFluidAmount() == 1000, "Actual screen has nine components, two bucket slots and native default tank");
                for (int slot = 0; slot < 9; slot++) check(container.getSlots().get(slot).x == tab.getX() - screen.getGuiLeft() + 5 + slot % 3 * 18
                        && container.getSlots().get(slot).y == tab.getY() - screen.getGuiTop() + 139 + slot / 3 * 18, "Reuse the controller's 3x3 layout");
                capture(event, "alloyer-ui");
                playerSlot(screen);
            }
            case 4 -> { if (!carried.is(Items.BUCKET)) return; click(screen, tab.getX() + 30, tab.getY() + 44, 0); }
            case 5 -> { if (!carried.is(Items.WATER_BUCKET)) return; click(screen, tab.getX() + 15, tab.getY() + 85, 1); }
            case 6 -> {
                if (!carried.is(Items.BUCKET) || wrapper.getInputTank(0).getFluidInTank(0).getAmount() != 1000) return;
                check(wrapper.getTank().isEmpty(), "Direct default-tank output and ingredient-tank input synchronize from the server");
                playerSlot(screen);
            }
            case 7 -> {
                if (!carried.isEmpty()) return;
                var control = cardSwitch(screen, container);
                click(screen, control.getX() + 3, control.getY() + 3, 0);
            }
            case 8 -> {
                if (!wrapper.isHeating() || wrapper.getTank().getFluid().getFluid() != TinkerFluids.moltenRoseGold.get()) return;
                var furnace = screen.getMenu().getUpgradeContainers().values().stream().filter(MeltingUpgradeContainer.class::isInstance)
                        .map(MeltingUpgradeContainer.class::cast).findFirst().orElseThrow();
                check(!furnace.getUpgradeWrapper().isHeating(), "Alloyer switch is independent of the coinstalled foundry");
                if (Boolean.getBoolean("sophisticatedsmeltery.alloyerLiquidSmoke"))
                    check(wrapper.getInternalFuelTanks().get(0).getFluidInTank(0).getAmount() < 1000, "Actual liquid fuel was consumed while making the alloy");
                capture(event, "alloyer-ui-running");
                click(screen, tab.getX() + 8, tab.getY() + 12, 0);
                var control = cardSwitch(screen, container);
                click(screen, control.getX() + 3, control.getY() + 3, 0);
            }
            case 9 -> {
                if (wrapper.isHeating()) return;
                check(!container.isOpen(), "Card switch remains effective while the settings tab is closed");
                stoppedFuel = wrapper.alloyer().fuel().getFuel();
                click(screen, tab.getX() + 8, tab.getY() + 12, 0);
            }
            case 10 -> {
                if (!container.isOpen()) return;
                check(!wrapper.isHeating() && wrapper.alloyer().fuel().getFuel() == stoppedFuel, "The card switch pauses paid burn time");
                capture(event, "alloyer-card-switch-off");
                var fuelSlot = container.getSlots().get(8);
                click(screen, screen.getGuiLeft() + fuelSlot.x + 8, screen.getGuiTop() + fuelSlot.y + 8, 0);
            }
            case 11 -> {
                if (!wrapper.getComponents().getStackInSlot(8).isEmpty() || carried.isEmpty()) return;
                var hint = tab.children().stream().filter(child -> child instanceof net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase widget
                                && widget.getX() == tab.getX() + 41 && widget.getY() == tab.getY() + 175)
                        .map(net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase.class::cast).findFirst().orElseThrow();
                hint.renderTooltip(screen, event.getGuiGraphics(), tab.getX() + 49, tab.getY() + 183);
                capture(event, "alloyer-fuel-slot-hint");
                SophisticatedSmeltery.LOGGER.info("ALLOYER UI PASSED: independent foundry coexistence, 3x3 components, native tank, direct bucket output/input, upgrade-card switch, closed-tab pause with saved burn; liquidFuel={}", Boolean.getBoolean("sophisticatedsmeltery.alloyerLiquidSmoke"));
                stage = 12;
                mc.player.closeContainer();
                mc.stop();
                return;
            }
            default -> throw new IllegalStateException("Unexpected alloyer test stage " + stage);
        }
        stage++;
        frames = 0;
    }
    private static AlloyerUpgradeSwitch cardSwitch(StorageScreenBase<?> screen, AlloyerUpgradeContainer container) {
        int slot = screen.getMenu().getUpgradeContainers().entrySet().stream().filter(entry -> entry.getValue() == container)
                .findFirst().orElseThrow().getKey();
        return screen.children().stream().filter(AlloyerUpgradeSwitch.class::isInstance).map(AlloyerUpgradeSwitch.class::cast)
                .filter(control -> control.getX() == screen.getGuiLeft() - 22 && control.getY() == screen.getGuiTop() + 8 + slot * 16)
                .findFirst().orElseThrow();
    }
    private static void playerSlot(StorageScreenBase<?> screen) {
        var slot = screen.getMenu().slots.stream().filter(value -> value.container == Minecraft.getInstance().player.getInventory() && value.getSlotIndex() == 11).findFirst().orElseThrow();
        click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8, 0);
    }
    private static void click(StorageScreenBase<?> screen, int x, int y, int button) {
        check(screen.mouseClicked(x, y, button), "Screen dispatches alloyer interaction");
        screen.mouseReleased(x, y, button);
    }
    private static void capture(ScreenEvent.Render.Post event, String name) {
        event.getGuiGraphics().flush();
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) { image.writeToFile(Path.of("..", name + ".png")); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
