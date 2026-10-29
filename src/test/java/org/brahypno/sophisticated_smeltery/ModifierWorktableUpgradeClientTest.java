package org.brahypno.sophisticated_smeltery;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import org.brahypno.sophisticated_smeltery.client.ModifierWorktableUpgradeTab;
import org.brahypno.sophisticated_smeltery.client.TinkerStationToolInfo;
import org.brahypno.sophisticated_smeltery.upgrade.ModifierWorktableUpgradeContainer;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.table.ModifierWorktableBlockEntity;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class ModifierWorktableUpgradeClientTest {
    private static int stage, ticks, frames;
    private static final boolean storageHost = "storage".equals(System.getProperty("sophisticatedsmeltery.modifierWorktableHost"));
    private static ItemStack expected = ItemStack.EMPTY;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.modifierWorktableSmoke") || event.phase != TickEvent.Phase.END || stage >= 20) return;
        var mc = Minecraft.getInstance();
        if (++ticks > 6000) throw new IllegalStateException("Modifier worktable UI timed out at " + stage);
        if (stage == 0 && mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) mc.setScreen(new TitleScreen());
        if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            mc.options.guiScale().set(2);
            mc.options.pauseOnLostFocus = false;
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 800);
            stage = 1;
            mc.createWorldOpenFlows().createFreshLevel("modifier-worktable-test-" + System.currentTimeMillis(),
                    new LevelSettings("Modifier worktable UI test", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42L, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        } else if (stage == 1 && mc.player != null && mc.screen == null && mc.getConnection() != null) {
            mc.getConnection().sendCommand("ssmeltery_test " + (storageHost ? "storage_modifier_worktable" : "modifier_worktable")); stage = 2;
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void render(ScreenEvent.Render.Post event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.modifierWorktableSmoke") || stage < 2 || stage >= 15
                || !(event.getScreen() instanceof StorageScreenBase<?> screen) || ++frames < 15) return;
        var mc = Minecraft.getInstance();
        var container = screen.getMenu().getUpgradeContainers().values().stream().filter(ModifierWorktableUpgradeContainer.class::isInstance)
                .map(ModifierWorktableUpgradeContainer.class::cast).findFirst().orElseThrow();
        var wrapper = container.getUpgradeWrapper();
        var tab = screen.getUpgradeSettingsControl().children().stream().filter(ModifierWorktableUpgradeTab.class::isInstance)
                .map(ModifierWorktableUpgradeTab.class::cast).findFirst().orElseThrow();
        event.getGuiGraphics().flush();
        switch (stage) {
            case 2 -> { if (!container.isOpen()) click(screen, tab.getX() + 8, tab.getY() + 12); }
            case 3 -> {
                if (!container.isOpen()) return;
                check(container.getSlots().size() == 9 && !screen.getMenu().canDisableUpgrade(0), "Three inputs, one output and player equipment without a tick switch");
                check(tab.getY() + 175 <= mc.getWindow().getGuiScaledHeight(), "The compact tab fits the screen");
                check(tab.information().getX() + tab.information().getWidth() <= screen.getGuiLeft(), "Native information panels appear to the left of storage");
                verifyNativeInformation(tab, container);
                int arrowLeft = tab.getX() + 71, arrowRight = arrowLeft + 22;
                check(screen.getGuiLeft() + container.getSlots().get(2).x + 16 < arrowLeft
                        && arrowRight < screen.getGuiLeft() + container.resultSlot().x - 4, "The full native arrow fits between the ingredient and result frames");
                for (int i = 0; i < container.getSlots().size(); i++) for (int j = i + 1; j < container.getSlots().size(); j++) {
                    var a = container.getSlots().get(i); var b = container.getSlots().get(j);
                    check(Math.abs(a.x - b.x) >= 16 || Math.abs(a.y - b.y) >= 16, "Item slots have distinct click targets");
                }
                capture("empty"); clickPlayerSlot(screen, 11);
            }
            case 4 -> { if (screen.getMenu().getCarried().isEmpty()) return; clickUpgradeSlot(screen, container, 0); }
            case 5 -> { if (wrapper.getTinkerableStack().isEmpty()) return; clickPlayerSlot(screen, 12); }
            case 6 -> { if (screen.getMenu().getCarried().isEmpty()) return; clickUpgradeSlot(screen, container, 1); }
            case 7 -> {
                if (wrapper.options().isEmpty()) return;
                verifyNativeInformation(tab, container);
                capture("recipe-description");
                int rows = Math.max(0, (wrapper.options().size() + 3) / 4 - 4);
                check(rows > 0, "More than sixteen native modifiers exercise scrolling");
                int scrollX = tab.getX() + 89, scrollY = tab.getY() + 26;
                check(screen.mouseClicked(scrollX, scrollY + 7, 0), "Grab modifier scrollbar");
                check(screen.mouseDragged(scrollX, scrollY + 65, 0, 0, 58), "Drag modifier scrollbar");
                screen.mouseReleased(scrollX, scrollY + 65, 0);
                click(screen, tab.getX() + 17, tab.getY() + 35);
                check(wrapper.selected() == rows * 4, "Scrolled icon selects its native modifier index");
                check(screen.mouseScrolled(tab.getX() + 20, tab.getY() + 40, rows), "Wheel returns to the first modifier row");
                click(screen, tab.getX() + 17, tab.getY() + 35);
            }
            case 8 -> {
                if (wrapper.result().isEmpty() || container.resultSlot().getItem().isEmpty()) return;
                verifyNativeInformation(tab, container);
                verifyDragging(screen, tab);
                expected = wrapper.result(); capture("selected"); clickUpgradeSlot(screen, container, 3);
            }
            case 9 -> {
                if (screen.getMenu().getCarried().isEmpty()) return;
                check(ItemStack.matches(loaded(screen.getMenu().getCarried()), loaded(expected)) && wrapper.getTinkerableStack().isEmpty() && wrapper.getInput(0).is(Items.COMPASS),
                        "Server returns exact native result and retains sorting catalyst");
                clickPlayerSlot(screen, 13);
            }
            case 10 -> { if (!screen.getMenu().getCarried().isEmpty()) return; clickPlayerSlot(screen, 13); }
            case 11 -> { if (screen.getMenu().getCarried().isEmpty()) return; clickUpgradeSlot(screen, container, 0); }
            case 12 -> {
                if (wrapper.getTinkerableStack().isEmpty()) return;
                click(screen, tab.getX() + 17, tab.getY() + 35);
                click(screen, tab.getX() + 16, tab.getY() + 159);
            }
            case 13 -> {
                if (!wrapper.shiftIntoStorage() || wrapper.result().isEmpty()) return;
                expected = wrapper.result();
                mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, container.resultSlot().index, 0, ClickType.QUICK_MOVE, mc.player);
            }
            case 14 -> {
                int count = 0;
                var inventory = screen.getMenu().getStorageWrapper().getInventoryHandler();
                for (int slot = 0; slot < inventory.getSlots(); slot++) if (ItemStack.isSameItemSameTags(loaded(inventory.getStackInSlot(slot)), loaded(expected))) count += inventory.getStackInSlot(slot).getCount();
                if (count == 0) return;
                check(count == expected.getCount() && wrapper.getTinkerableStack().isEmpty() && wrapper.getInput(0).is(Items.COMPASS), "Shift destination and native nonconsuming catalyst synchronize");
                capture("crafted");
                SophisticatedSmeltery.LOGGER.info("MODIFIER WORKTABLE UI PASSED: {}, native message/result panel states, left floating panels and dragging, complete arrow, scrollbar drag and wheel, real input clicks, selected result, exact consumption and Shift destination", storageHost ? "storage" : "backpack");
                stage = 20; mc.player.closeContainer(); mc.stop(); return;
            }
            default -> { return; }
        }
        stage++; frames = 0;
    }
    private static ItemStack loaded(ItemStack stack) {
        // Tinkers normalizes tool tags on load, including an implicit false broken flag.
        return ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));
    }
    private static void verifyNativeInformation(ModifierWorktableUpgradeTab tab, ModifierWorktableUpgradeContainer container) {
        var mc = Minecraft.getInstance();
        var wrapper = container.getUpgradeWrapper();
        var nativeTable = new ModifierWorktableBlockEntity(BlockPos.ZERO, TinkerTables.modifierWorktable.get().defaultBlockState());
        nativeTable.setLevel(mc.level);
        for (int slot = 0; slot < 3; slot++) nativeTable.setItem(slot, wrapper.inventory().getStackInSlot(slot).copy());
        nativeTable.getCurrentRecipe();
        nativeTable.selectModifier(wrapper.selected());
        var expected = nativeTable.calcResult(mc.player);
        check(ItemStack.matches(expected, wrapper.result()), "Preview matches the native worktable");
        var information = tab.information();
        var message = nativeTable.getCurrentMessage();
        if (!message.getString().isEmpty() || expected.isEmpty()) {
            var recipe = nativeTable.getCurrentRecipe();
            var title = recipe == null ? net.minecraft.network.chat.Component.translatable("gui.tconstruct.modifier_worktable.title") : recipe.getTitle();
            var description = message.getString().isEmpty() ? net.minecraft.network.chat.Component.translatable("gui.tconstruct.modifier_worktable.info") : message;
            check(information.stats().caption().equals(title) && information.stats().entries().size() == 1
                    && information.stats().entries().get(0).text().equals(description) && information.modifiers().entries().isEmpty(), "Instruction/recipe messages and empty lower panel match the native screen");
        } else {
            check(information.stats().entries().equals(TinkerStationToolInfo.stats(expected, mc.player))
                    && information.modifiers().entries().equals(TinkerStationToolInfo.modifiers(expected, mc.player, false, false)), "Successful selection replaces instructions with native result stats and modifiers");
        }
    }
    private static void verifyDragging(StorageScreenBase<?> screen, ModifierWorktableUpgradeTab tab) {
        var panel = tab.information();
        int left = panel.getX(), top = panel.getY(), separation = panel.modifiers().getY() - panel.stats().getY();
        int startX = left + 70, startY = top + 7;
        var pressed = new ScreenEvent.MouseButtonPressed.Pre(screen, startX, startY, 0);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(pressed);
        var dragged = new ScreenEvent.MouseDragged.Pre(screen, startX + 10, startY + 10, 0, 10, 10);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(dragged);
        var released = new ScreenEvent.MouseButtonReleased.Pre(screen, startX + 10, startY + 10, 0);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(released);
        check(pressed.isCanceled() && dragged.isCanceled() && released.isCanceled()
                && panel.getX() == left + 10 && panel.getY() == top + 10
                && panel.modifiers().getY() - panel.stats().getY() == separation, "Title dragging moves both panels together and consumes inventory input");
        panel.mouseClicked(panel.getX() + 70, panel.getY() + 7, 0);
        panel.mouseDragged(left + 70, top + 7, 0, -10, -10); panel.mouseReleased(left + 70, top + 7, 0);
    }
    private static void clickPlayerSlot(StorageScreenBase<?> screen, int inventoryIndex) {
        var player = Minecraft.getInstance().player;
        var slot = screen.getMenu().slots.stream().filter(value -> value.container == player.getInventory() && value.getSlotIndex() == inventoryIndex).findFirst().orElseThrow();
        click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
    }
    private static void clickUpgradeSlot(StorageScreenBase<?> screen, ModifierWorktableUpgradeContainer container, int index) {
        var slot = container.getSlots().get(index); click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
    }
    private static void click(StorageScreenBase<?> screen, int x, int y) { check(screen.mouseClicked(x, y, 0), "Actual screen interaction"); screen.mouseReleased(x, y, 0); }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static void capture(String name) {
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) { image.writeToFile(Path.of("..", "modifier-worktable-" + (storageHost ? "storage-" : "backpack-") + name + ".png")); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
}
