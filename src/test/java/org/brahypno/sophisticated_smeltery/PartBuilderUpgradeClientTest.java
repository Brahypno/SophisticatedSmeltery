package org.brahypno.sophisticated_smeltery;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
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
import org.brahypno.sophisticated_smeltery.client.PartBuilderUpgradeTab;
import org.brahypno.sophisticated_smeltery.upgrade.PartBuilderUpgradeContainer;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class PartBuilderUpgradeClientTest {
    private static int stage, ticks, frames, used, shiftCrafts;
    private static ItemStack expected = ItemStack.EMPTY;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.partBuilderSmoke") || event.phase != TickEvent.Phase.END || stage >= 20) return;
        var mc = Minecraft.getInstance();
        if (++ticks > 6000) throw new IllegalStateException("Part builder UI timed out at " + stage);
        if (stage == 0 && mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) mc.setScreen(new TitleScreen());
        if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            mc.options.guiScale().set(2);
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
            stage = 1;
            mc.createWorldOpenFlows().createFreshLevel("part-builder-test-" + System.currentTimeMillis(),
                    new LevelSettings("Part builder UI test", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42L, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        } else if (stage == 1 && mc.player != null && mc.screen == null && mc.getConnection() != null) {
            mc.getConnection().sendCommand("ssmeltery_test part_builder"); stage = 2;
        } else if (stage == 15 && mc.screen == null) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND); stage = 16;
        }
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.partBuilderSmoke") || stage < 2 || stage >= 20
                || !(event.getScreen() instanceof StorageScreenBase<?> screen) || ++frames < 15) return;
        var mc = Minecraft.getInstance();
        var container = screen.getMenu().getUpgradeContainers().values().stream().filter(PartBuilderUpgradeContainer.class::isInstance)
                .map(PartBuilderUpgradeContainer.class::cast).findFirst().orElseThrow();
        var wrapper = container.getUpgradeWrapper();
        var tab = screen.getUpgradeSettingsControl().children().stream().filter(PartBuilderUpgradeTab.class::isInstance)
                .map(PartBuilderUpgradeTab.class::cast).findFirst().orElseThrow();
        switch (stage) {
            case 2 -> click(screen, tab.getX() + 8, tab.getY() + 12);
            case 3 -> {
                if (!container.isOpen()) return;
                check(container.getSlots().size() == 3 && !screen.getMenu().canDisableUpgrade(0), "Two inputs and one result, with no tick switch");
                capture("part-builder-empty");
                clickPlayerSlot(screen, 11);
            }
            case 4 -> { if (screen.getMenu().getCarried().isEmpty()) return; clickUpgradeSlot(screen, container, 0); }
            case 5 -> { if (wrapper.getPatternStack().isEmpty()) return; clickPlayerSlot(screen, 12); }
            case 6 -> { if (screen.getMenu().getCarried().isEmpty()) return; clickUpgradeSlot(screen, container, 1); }
            case 7 -> {
                if (wrapper.getStack().isEmpty()) return;
                int hiddenRows = Math.max(0, (wrapper.patterns().size() + 3) / 4 - 4);
                check(hiddenRows > 0, "Native wood patterns exercise the scrollbar");
                int scrollX = tab.getX() + 89;
                int scrollY = tab.getY() + 26;
                check(screen.mouseClicked(scrollX, scrollY + 7, 0), "Grab native scroll thumb");
                check(screen.mouseDragged(scrollX, scrollY + 65, 0, 0, 58), "Drag native scroll thumb to bottom");
                screen.mouseReleased(scrollX, scrollY + 65, 0);
                capture("part-builder-scrolled");
                click(screen, tab.getX() + 17, tab.getY() + 35);
                check(wrapper.patterns().get(hiddenRows * 4).equals(wrapper.selected()), "Dragging exposes the final pattern row");
                check(screen.mouseScrolled(tab.getX() + 20, tab.getY() + 40, hiddenRows), "Mouse wheel scrolls back to top");
                click(screen, tab.getX() + 17, tab.getY() + 35);
                check(wrapper.patterns().get(0).equals(wrapper.selected()), "Mouse wheel restores the first pattern row");
                var pattern = new Pattern("tconstruct:pick_head");
                int index = wrapper.patterns().indexOf(pattern);
                check(index >= 0, "Native pick head is offered");
                int rows = Math.max(0, index / 4 - 3);
                for (int row = 0; row < rows; row++) tab.mouseScrolled(tab.getX() + 20, tab.getY() + 40, -1);
                int relative = index - rows * 4;
                click(screen, tab.getX() + 8 + relative % 4 * 18 + 9, tab.getY() + 26 + relative / 4 * 18 + 9);
            }
            case 8 -> {
                if (wrapper.result().isEmpty() || container.getSlots().get(2).getItem().isEmpty()) return;
                expected = wrapper.result().copy(); used = wrapper.selectedRecipe().getItemsUsed(wrapper);
                capture("part-builder-selected");
                clickUpgradeSlot(screen, container, 2);
            }
            case 9 -> {
                if (screen.getMenu().getCarried().isEmpty()) return;
                check(ItemStack.isSameItemSameTags(screen.getMenu().getCarried(), expected) && wrapper.getStack().getCount() == 16 - used
                        && wrapper.getPatternStack().getCount() == 15, "Taking a part consumes exactly one recipe on the server");
                clickPlayerSlot(screen, 13);
            }
            case 10 -> { if (!screen.getMenu().getCarried().isEmpty()) return; click(screen, tab.getX() + 16, tab.getY() + 153); }
            case 11 -> {
                if (!wrapper.shiftIntoStorage()) return;
                shiftCrafts = Math.min(expected.getMaxStackSize() / expected.getCount(),
                        Math.min(wrapper.getStack().getCount() / used, wrapper.getPatternStack().getCount()));
                mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, container.getSlots().get(2).index, 0, ClickType.QUICK_MOVE, mc.player);
            }
            case 12 -> {
                var storage = screen.getMenu().getStorageWrapper().getInventoryHandler();
                int count = 0;
                for (int slot = 0; slot < storage.getSlots(); slot++) if (ItemStack.isSameItemSameTags(storage.getStackInSlot(slot), expected)) count += storage.getStackInSlot(slot).getCount();
                if (count == 0) return;
                check(count == shiftCrafts * expected.getCount() && wrapper.getStack().getCount() == 16 - used * (shiftCrafts + 1)
                        && wrapper.getPatternStack().getCount() == 15 - shiftCrafts,
                        "Shift batch matches native input consumption: parts=" + count + ", material=" + wrapper.getStack().getCount() + ", patterns=" + wrapper.getPatternStack().getCount());
                capture("part-builder-crafted");
                mc.player.closeContainer(); stage = 15; frames = 0; return;
            }
            case 16 -> {
                check(wrapper.shiftIntoStorage() && new Pattern("tconstruct:pick_head").equals(wrapper.selected())
                        && wrapper.getStack().getCount() == 16 - used * (shiftCrafts + 1) && wrapper.getPatternStack().getCount() == 15 - shiftCrafts, "Reopening preserves inputs, selection, and destination");
                if (!container.isOpen()) click(screen, tab.getX() + 8, tab.getY() + 12);
            }
            case 17 -> {
                if (!container.isOpen()) return;
                capture("part-builder-reopened");
                SophisticatedSmeltery.LOGGER.info("PART BUILDER UI PASSED: native scrollbar drag and wheel, real input clicks, pattern selection, output pickup, exact native consumption, Shift destination, saved inputs and selection, reopening");
                stage = 20; mc.player.closeContainer(); mc.stop(); return;
            }
            default -> { return; }
        }
        stage++; frames = 0;
    }
    private static void clickPlayerSlot(StorageScreenBase<?> screen, int inventoryIndex) {
        var mc = Minecraft.getInstance();
        var slot = screen.getMenu().slots.stream().filter(value -> value.container == mc.player.getInventory() && value.getSlotIndex() == inventoryIndex).findFirst().orElseThrow();
        click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
    }
    private static void clickUpgradeSlot(StorageScreenBase<?> screen, PartBuilderUpgradeContainer container, int index) {
        var slot = container.getSlots().get(index);
        click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
    }
    private static void click(StorageScreenBase<?> screen, int x, int y) { check(screen.mouseClicked(x, y, 0), "Actual screen interaction"); screen.mouseReleased(x, y, 0); }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static void capture(String name) {
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) { image.writeToFile(Path.of("..", name + ".png")); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
}

