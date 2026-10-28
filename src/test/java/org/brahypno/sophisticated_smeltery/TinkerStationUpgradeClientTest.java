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
import org.brahypno.sophisticated_smeltery.client.TinkerStationUpgradeTab;
import org.brahypno.sophisticated_smeltery.client.TinkerStationInformationPanel;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeContainer;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class TinkerStationUpgradeClientTest {
    private static int stage, ticks, frames;
    private static boolean advanced, inspectingPreview;
    private static int equipmentStep;
    private static final java.util.List<net.minecraft.world.item.Item> EQUIPMENT = java.util.List.of(
            net.minecraft.world.item.Items.DIAMOND_HELMET, net.minecraft.world.item.Items.DIAMOND_CHESTPLATE,
            net.minecraft.world.item.Items.DIAMOND_LEGGINGS, net.minecraft.world.item.Items.DIAMOND_BOOTS, net.minecraft.world.item.Items.SHIELD);
    private static net.minecraft.nbt.CompoundTag informationInputs;
    private static ItemStack expected = ItemStack.EMPTY;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.tinkerStationSmoke") || event.phase != TickEvent.Phase.END || stage >= 40) return;
        var mc = Minecraft.getInstance();
        if (++ticks > 7200) throw new IllegalStateException("Workstation UI timed out at " + stage);
        if (stage == 0 && mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) mc.setScreen(new TitleScreen());
        if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            mc.options.guiScale().set(2);
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
            stage = 1;
            mc.createWorldOpenFlows().createFreshLevel("tinker-station-test-" + System.currentTimeMillis(),
                    new LevelSettings("Small backpack workstation test", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42L, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        } else if (stage == 1 && mc.player != null && mc.screen == null && mc.getConnection() != null) {
            mc.getConnection().sendCommand("ssmeltery_test " + (advanced ? "tinkers_anvil" : "tinker_station")); stage = 2;
        } else if (stage == 20 && mc.screen == null) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND); stage = 21;
        }
    }
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void render(ScreenEvent.Render.Post event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.tinkerStationSmoke") || stage < 2 || stage >= 40
                || !(event.getScreen() instanceof StorageScreenBase<?> screen) || ++frames < 15) return;
        var mc = Minecraft.getInstance();
        var container = screen.getMenu().getUpgradeContainers().values().stream().filter(TinkerStationUpgradeContainer.class::isInstance)
                .map(TinkerStationUpgradeContainer.class::cast).findFirst().orElseThrow();
        var wrapper = container.getUpgradeWrapper();
        var tab = screen.getUpgradeSettingsControl().children().stream().filter(TinkerStationUpgradeTab.class::isInstance)
                .map(TinkerStationUpgradeTab.class::cast).findFirst().orElseThrow();
        event.getGuiGraphics().flush();
        switch (stage) {
            case 2 -> click(screen, tab.getX() + 8, tab.getY() + 12);
            case 3 -> {
                if (!container.isOpen()) return;
                check(container.getSlots().size() == (advanced ? 12 : 10), "Tier crafting slots plus five real player equipment slots");
                check(tab.getX() >= 0 && tab.getY() >= 0 && tab.getX() + tab.getWidth() <= screen.width
                        && tab.getY() + tab.getHeight() <= screen.height, "Complete sidebar fits the small backpack screen");
                check(tab.information().getX() + tab.information().getWidth() <= screen.getGuiLeft() - 25,
                        "Information floats to the left without covering the backpack controls");
                verifySlotBounds(screen, tab, container);
                capture(advanced ? "tinkers-anvil-small-empty" : "tinker-station-small-empty");
                var layouts = wrapper.layouts();
                if (layouts.size() > TinkerStationUpgradeTab.LAYOUT_COLUMNS * TinkerStationUpgradeTab.LAYOUT_ROWS) {
                    check(tab.mouseScrolled(tab.getX() + 20, tab.getY() + 107, -1), "Wheel scrolling reaches the fifth picker row");
                    check(tab.mouseScrolled(tab.getX() + 20, tab.getY() + 107, 1), "Wheel scrolling returns to the first layouts without arrow buttons");
                }
                int index = Math.min(layouts.size(), TinkerStationUpgradeTab.LAYOUT_COLUMNS * TinkerStationUpgradeTab.LAYOUT_ROWS) - 1;
                click(screen, tab.getX() + 17 + index % TinkerStationUpgradeTab.LAYOUT_COLUMNS * 18,
                        tab.getY() + 35 + index / TinkerStationUpgradeTab.LAYOUT_COLUMNS * 18);
                stage = 31; frames = 0; return;
            }
            case 31 -> {
                var layouts = wrapper.layouts();
                int last = Math.min(layouts.size(), TinkerStationUpgradeTab.LAYOUT_COLUMNS * TinkerStationUpgradeTab.LAYOUT_ROWS) - 1;
                if (!wrapper.layout().getName().equals(layouts.get(last).getName())) return;
                check(last / TinkerStationUpgradeTab.LAYOUT_COLUMNS >= 2, "Expanded five-column picker selects a real native layout in its lower rows");
                verifySlotBounds(screen, tab, container);
                capture(advanced ? "tinkers-anvil-expanded-picker" : "tinker-station-expanded-picker");
                int index = java.util.stream.IntStream.range(0, layouts.size()).filter(i -> layouts.get(i).getName().toString().equals("tconstruct:pickaxe")).findFirst().orElseThrow();
                int rows = Math.max(0, index / TinkerStationUpgradeTab.LAYOUT_COLUMNS - TinkerStationUpgradeTab.LAYOUT_ROWS + 1);
                for (int row = 0; row < rows; row++) tab.mouseScrolled(tab.getX() + 20, tab.getY() + 40, -1);
                int relative = index - rows * TinkerStationUpgradeTab.LAYOUT_COLUMNS;
                click(screen, tab.getX() + 17 + relative % TinkerStationUpgradeTab.LAYOUT_COLUMNS * 18,
                        tab.getY() + 35 + relative / TinkerStationUpgradeTab.LAYOUT_COLUMNS * 18);
                stage = 4; frames = 0; return;
            }
            case 4 -> {
                if (!wrapper.layout().getName().toString().equals("tconstruct:pickaxe")) return;
                verifySlotBounds(screen, tab, container);
                check(tab.information().stats().caption().equals(wrapper.layout().getDisplayName()), "Upper panel shows the native selected layout");
                check(tab.information().modifiers().caption().equals(net.minecraft.network.chat.Component.translatable("gui.tconstruct.tinker_station.components")),
                        "Lower panel shows native required components before a tool exists");
                var components = tab.information().modifiers().entries();
                check(components.size() == 3 && components.stream().allMatch(entry ->
                        net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.RED).equals(entry.text().getStyle().getColor())),
                        "All three missing pickaxe components are marked red");
                capture((advanced ? "tinkers-anvil" : "tinker-station") + "-required-components");
                equipmentStep = 0; stage = 32; frames = 0; return;
            }
            case 32 -> {
                if (equipmentStep < EQUIPMENT.size() * 2) {
                    int index = equipmentStep / 2;
                    if (equipmentStep % 2 == 0) {
                        if (!screen.getMenu().getCarried().isEmpty()) return;
                        clickPlayerSlot(screen, 15 + index);
                    } else {
                        if (screen.getMenu().getCarried().isEmpty()) return;
                        var slot = container.equipmentSlots().get(index);
                        click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
                    }
                    equipmentStep++; frames = 0; return;
                }
                if (!screen.getMenu().getCarried().isEmpty()) return;
                verifyEquipped(container);
                capture((advanced ? "tinkers-anvil" : "tinker-station") + "-equipment");
                clickPlayerSlot(screen, 11); stage = 5; frames = 0; return;
            }
            case 5 -> { if (screen.getMenu().getCarried().isEmpty()) return; clickUpgradeSlot(screen, container, 1); }
            case 6 -> {
                if (wrapper.getInput(0).isEmpty()) return;
                check(tab.information().modifiers().entries().get(0).text().getStyle().getColor() == null,
                        "Lower component list updates when the required part is inserted");
                clickPlayerSlot(screen, 12);
            }
            case 7 -> { if (screen.getMenu().getCarried().isEmpty()) return; clickUpgradeSlot(screen, container, 2); }
            case 8 -> { if (wrapper.getInput(1).isEmpty()) return; clickPlayerSlot(screen, 13); }
            case 9 -> { if (screen.getMenu().getCarried().isEmpty()) return; clickUpgradeSlot(screen, container, 3); }
            case 10 -> {
                if (wrapper.result().isEmpty()) return;
                click(screen, tab.getX() + 40, tab.getY() + 211);
                for (char c : "Small pick".toCharArray()) check(screen.charTyped(c, 0), "Name field accepts typing");
            }
            case 11 -> {
                if (!wrapper.result().getHoverName().getString().equals("Small pick")) return;
                expected = wrapper.result().copy();
                capture(advanced ? "tinkers-anvil-small-selected" : "tinker-station-small-selected");
                informationInputs = wrapper.inventory().serializeNBT(); inspectingPreview = true;
                stage = 24; frames = 0; return;
            }
            case 12 -> {
                if (screen.getMenu().getCarried().isEmpty()) return;
                check(ItemStack.matches(screen.getMenu().getCarried(), expected) && wrapper.getInput(0).getCount() == 3, "Named result and exact server consumption");
                clickPlayerSlot(screen, 14);
            }
            case 13 -> { if (!screen.getMenu().getCarried().isEmpty()) return; click(screen, tab.getX() + 16, tab.getY() + 126); }
            case 14 -> {
                if (!wrapper.shiftIntoStorage()) return;
                mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, container.resultSlot().index, 0, ClickType.QUICK_MOVE, mc.player);
            }
            case 15 -> {
                int count = 0;
                var inventory = screen.getMenu().getStorageWrapper().getInventoryHandler();
                for (int i = 0; i < inventory.getSlots(); i++) if (inventory.getStackInSlot(i).is(expected.getItem())) count += inventory.getStackInSlot(i).getCount();
                if (count == 0) return;
                check(count == 1 && wrapper.getInput(0).getCount() == 2, "Shift result goes to small backpack storage");
                mc.player.closeContainer(); stage = 20; frames = 0; return;
            }
            case 21 -> {
                verifyEquipped(container);
                check(wrapper.shiftIntoStorage() && wrapper.getInput(0).getCount() == 2
                        && wrapper.layout().getName().toString().equals("tconstruct:pickaxe"), "Reopening preserves inputs, layout and destination");
                if (!container.isOpen()) click(screen, tab.getX() + 8, tab.getY() + 12);
            }
            case 22 -> {
                if (!container.isOpen()) return;
                capture(advanced ? "tinkers-anvil-small-reopened" : "tinker-station-small-reopened");
                mc.player.closeContainer(); frames = 0;
                if (!advanced) { advanced = true; stage = 1; return; }
                stage = 29;
                mc.getConnection().sendCommand("ssmeltery_test tinker_info"); return;
            }
            case 24 -> {
                verifySlotBounds(screen, tab, container);
                check(container.resultSlot().x >= 0,
                        "Crafting output stays visible alongside information");
                var information = tab.information();
                check(information.stats().getY() + information.stats().getHeight() < information.modifiers().getY()
                        && information.modifiers().getY() + information.modifiers().getHeight() <= screen.height,
                        "Both native panels are visible together, stacked inside the screen");
                ItemStack displayed = org.brahypno.sophisticated_smeltery.client.TinkerStationToolInfo.displayedTool(wrapper);
                check(ItemStack.matches(displayed, inspectingPreview ? wrapper.result() : wrapper.getTinkerableStack()), "Information selects the native preview or the input tool");
                verifyNativeInformation(displayed);
                check(information.stats().entries().equals(org.brahypno.sophisticated_smeltery.client.TinkerStationToolInfo.stats(displayed, mc.player))
                        && information.modifiers().entries().equals(org.brahypno.sophisticated_smeltery.client.TinkerStationToolInfo.modifiers(displayed, mc.player, false, false)),
                        "Upper stats and lower modifiers both display the full native data");
                capture((advanced ? "tinkers-anvil" : "tinker-station") + (inspectingPreview ? "-preview-panels" : "-input-panels"));
                if (!inspectingPreview) verifyDragging(screen, tab.information(), wrapper);
            }
            case 25 -> {
                if (!inspectingPreview) {
                    int before = tab.information().modifiers().offset(), statsBefore = tab.information().stats().offset();
                    scroll(screen, tab.information().modifiers(), -1);
                    check(tab.information().modifiers().offset() > before && tab.information().stats().offset() == statsBefore,
                            "Lower modifiers scroll independently of the upper stats");
                    int modifiersBefore = tab.information().modifiers().offset();
                    scroll(screen, tab.information().stats(), -1);
                    check(tab.information().modifiers().offset() == modifiersBefore, "Upper scrolling leaves the lower panel in place");
                }
            }
            case 26 -> {
                if (!inspectingPreview) {
                    capture("tinkers-anvil-input-modifiers-scrolled");
                    scroll(screen, tab.information().modifiers(), 1);
                    int before = tab.information().modifiers().offset();
                    dragScrollbar(screen, tab.information().modifiers(), true);
                    check(tab.information().modifiers().offset() > before, "Native lower scrollbar can be dragged through the modifier list");
                }
            }
            case 27 -> {
                check(informationInputs.equals(wrapper.inventory().serializeNBT()), "Reading both panels never changes crafting inputs");
                if (!inspectingPreview) {
                    capture("tinkers-anvil-input-modifiers-dragged");
                    dragScrollbar(screen, tab.information().modifiers(), false);
                    scroll(screen, tab.information().stats(), 1);
                }
            }
            case 28 -> {
                check(container.resultSlot().x >= 0, "Both information panels leave crafting available");
                if (inspectingPreview) { clickUpgradeSlot(screen, container, wrapper.inventory().getSlots()); stage = 12; frames = 0; return; }
                capture("tinkers-anvil-information-returned");
                SophisticatedSmeltery.LOGGER.info("TINKER STATION UI PASSED: both tiers equip all four armor pieces and offhand through real slot clicks, equipment survives closing/reopening, wheel-only five-row picker, relocated destination button, native panels, dragging, naming and crafting");
                stage = 40; mc.player.closeContainer(); mc.stop(); return;
            }
            case 29 -> {
                inspectingPreview = false;
                informationInputs = wrapper.inventory().serializeNBT();
                if (!container.isOpen()) click(screen, tab.getX() + 8, tab.getY() + 12);
            }
            case 30 -> {
                if (!container.isOpen()) return;
                var layouts = wrapper.layouts();
                int index = java.util.stream.IntStream.range(0, layouts.size())
                        .filter(i -> layouts.get(i).getName().toString().equals("tconstruct:pickaxe")).findFirst().orElseThrow();
                click(screen, tab.getX() + 17 + index % TinkerStationUpgradeTab.LAYOUT_COLUMNS * 18,
                        tab.getY() + 35 + index / TinkerStationUpgradeTab.LAYOUT_COLUMNS * 18);
                stage = 33; frames = 0; return;
            }
            case 33 -> {
                if (!wrapper.layout().getName().toString().equals("tconstruct:pickaxe")) return;
                var retained = container.getSlots().get(0);
                check(screen.getGuiLeft() + retained.x >= tab.getX() + 32
                                && screen.getGuiTop() + retained.y == tab.getY() + 118 && !retained.getItem().isEmpty(),
                        "Switching recipes with a tool inserted keeps it accessible beside the relocated green button");
                verifySlotBounds(screen, tab, container);
                capture("tinkers-anvil-equipped-layout-switch");
                click(screen, tab.getX() + 17, tab.getY() + 35);
                stage = 34; frames = 0; return;
            }
            case 34 -> {
                if (!wrapper.layout().getName().toString().equals("tconstruct:tinkers_anvil")) return;
                stage = 24; frames = 0; return;
            }
            default -> { return; }
        }
        stage++; frames = 0;
    }
    private static void verifySlotBounds(StorageScreenBase<?> screen, TinkerStationUpgradeTab tab, TinkerStationUpgradeContainer container) {
        int pickerBottom = tab.getY() + 26 + TinkerStationUpgradeTab.LAYOUT_ROWS * 18;
        for (var slot : container.getSlots()) {
            if (slot.x < 0) continue;
            int left = screen.getGuiLeft() + slot.x, top = screen.getGuiTop() + slot.y;
            boolean equipment = container.equipmentSlots().contains(slot);
            if (equipment) check(left >= tab.getX() + 8 + TinkerStationUpgradeTab.LAYOUT_COLUMNS * 18 + 4,
                    "Equipment column stays beside all five tool columns");
            check(left >= tab.getX() && left + 16 <= tab.getX() + tab.getWidth()
                            && top >= (equipment ? tab.getY() + 26 : pickerBottom) && top + 16 <= (equipment ? pickerBottom : tab.getY() + 200),
                    "Every visible crafting slot fits between the expanded picker and bottom controls");
        }
    }
    private static void verifyEquipped(TinkerStationUpgradeContainer container) {
        for (int index = 0; index < EQUIPMENT.size(); index++) {
            var slot = container.equipmentSlots().get(index);
            check(slot.getItem().is(EQUIPMENT.get(index)) && slot.container == Minecraft.getInstance().player.getInventory(),
                    "Equipment is stored directly in the matching player slot");
            if (index < 4) check(!slot.mayPlace(new ItemStack(net.minecraft.world.item.Items.DIRT)), "Armor slots reject ordinary items");
        }
    }
    private static void verifyNativeInformation(ItemStack stack) {
        var player = Minecraft.getInstance().player;
        var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack);
        var display = (slimeknights.tconstruct.library.tools.item.ITinkerStationDisplay)stack.getItem();
        var nativeStats = display.getStatInformation(tool, player, new java.util.ArrayList<>(),
                slimeknights.mantle.client.SafeClientAccess.getTooltipKey(), slimeknights.tconstruct.library.utils.TinkerTooltipFlags.TINKER_STATION);
        var stats = org.brahypno.sophisticated_smeltery.client.TinkerStationToolInfo.stats(stack, player);
        check(nativeStats.equals(stats.stream().map(org.brahypno.sophisticated_smeltery.client.TinkerStationToolInfo.Entry::text).toList()), "Every stat and formatted value matches the native station");
        var nativeModifiers = tool.getModifierList().stream().filter(entry -> entry.getModifier().shouldDisplay(slimeknights.tconstruct.library.modifiers.util.ModifierTooltip.TINKER_STATION))
                .map(entry -> entry.getModifier().getDisplayName(tool, entry, player.level().registryAccess())).toList();
        var modifiers = org.brahypno.sophisticated_smeltery.client.TinkerStationToolInfo.modifiers(stack, player, false, false);
        check(nativeModifiers.equals(modifiers.stream().map(org.brahypno.sophisticated_smeltery.client.TinkerStationToolInfo.Entry::text).toList()), "Native modifier names and levels match");
        check(modifiers.stream().allMatch(entry -> entry.tooltip() != null), "Native modifier descriptions are available");
        var upgrades = tool.getUpgrades().getModifiers().stream().filter(entry -> entry.getModifier().shouldDisplay(slimeknights.tconstruct.library.modifiers.util.ModifierTooltip.TINKER_STATION))
                .map(entry -> entry.getModifier().getDisplayName(tool, entry, player.level().registryAccess())).toList();
        check(upgrades.equals(org.brahypno.sophisticated_smeltery.client.TinkerStationToolInfo.modifiers(stack, player, true, false).stream()
                .map(org.brahypno.sophisticated_smeltery.client.TinkerStationToolInfo.Entry::text).toList()), "Shift modifier data matches the native upgrades filter");
    }
    private static void clickPlayerSlot(StorageScreenBase<?> screen, int inventoryIndex) {
        var mc = Minecraft.getInstance();
        var slot = screen.getMenu().slots.stream().filter(value -> value.container == mc.player.getInventory() && value.getSlotIndex() == inventoryIndex).findFirst().orElseThrow();
        click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
    }
    private static void clickUpgradeSlot(StorageScreenBase<?> screen, TinkerStationUpgradeContainer container, int index) {
        var slot = container.getSlots().get(index);
        check(slot.x >= 0 && slot.y >= 0, "Active slot is visible");
        click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
    }
    private static void click(StorageScreenBase<?> screen, int x, int y) {
        var pressed = new ScreenEvent.MouseButtonPressed.Pre(screen, x, y, 0);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(pressed);
        check(pressed.isCanceled() || screen.mouseClicked(x, y, 0), "Actual screen interaction");
        release(screen, x, y);
    }
    private static void release(StorageScreenBase<?> screen, int x, int y) {
        var released = new ScreenEvent.MouseButtonReleased.Pre(screen, x, y, 0);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(released);
        if (!released.isCanceled()) screen.mouseReleased(x, y, 0);
    }
    private static void scroll(StorageScreenBase<?> screen, TinkerStationInformationPanel.Section section, double delta) {
        var event = new ScreenEvent.MouseScrolled.Pre(screen, section.getX() + 50, section.getY() + 40, delta);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        check(event.isCanceled(), "Floating information consumes wheel scrolling before the inventory");
    }
    private static void dragScrollbar(StorageScreenBase<?> screen, TinkerStationInformationPanel.Section section, boolean bottom) {
        int x = section.getX() + section.getWidth() - 7, startY = section.getY() + 21;
        var pressed = new ScreenEvent.MouseButtonPressed.Pre(screen, x, startY, 0);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(pressed);
        check(pressed.isCanceled(), "Native scrollbar press is consumed");
        int endY = bottom ? section.getY() + section.getHeight() - 6 : startY;
        var dragged = new ScreenEvent.MouseDragged.Pre(screen, x, endY, 0, 0, endY - startY);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(dragged);
        check(dragged.isCanceled(), "Native scrollbar drag is consumed");
        release(screen, x, endY);
    }
    private static void verifyDragging(StorageScreenBase<?> screen, TinkerStationInformationPanel panel,
            org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeWrapper wrapper) {
        int left = panel.getX(), top = panel.getY();
        int separation = panel.modifiers().getY() - panel.stats().getY();
        drag(screen, panel, left + 20, top + 12);
        check(panel.getX() == left + 20 && panel.getY() == top + 12, "Title drag moves the panel");
        check(panel.stats().getY() == top + 12 && panel.modifiers().getY() - panel.stats().getY() == separation,
                "Dragging preserves both panel positions and their spacing");
        drag(screen, panel, screen.width + 100, screen.height + 100);
        check(panel.getX() + panel.getWidth() == screen.width && panel.getY() + panel.getHeight() == screen.height,
                "Dragged panel stays inside the bottom and right screen edges");
        drag(screen, panel, -100, -100);
        check(panel.getX() == 0 && panel.getY() == 0, "Dragged panel stays inside the top and left screen edges");
        var slot = screen.getMenu().slots.stream().filter(value -> value.container == Minecraft.getInstance().player.getInventory()
                && value.getSlotIndex() == 11).findFirst().orElseThrow();
        int slotX = screen.getGuiLeft() + slot.x + 8, slotY = screen.getGuiTop() + slot.y + 8;
        drag(screen, panel, slotX - 40, slotY - 80);
        check(panel.isMouseOver(slotX, slotY), "Floating panel covers a filled inventory slot for click-through check");
        ItemStack previous = slot.getItem().copy();
        click(screen, slotX, slotY);
        check(ItemStack.matches(previous, slot.getItem()) && screen.getMenu().getCarried().isEmpty(), "Floating panel consumes clicks over inventory slots");
        check(informationInputs.equals(wrapper.inventory().serializeNBT()), "Dragging never changes crafting inputs");
        drag(screen, panel, left, top);
        check(panel.getX() == left && panel.getY() == top, "Panel can be dragged back to the left");
        var stray = new ScreenEvent.MouseDragged.Pre(screen, left + 100, top + 50, 0, 50, 20);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(stray);
        check(!stray.isCanceled() && panel.getX() == left && panel.getY() == top, "Releasing the mouse ends dragging");
    }
    private static void drag(StorageScreenBase<?> screen, TinkerStationInformationPanel panel, int left, int top) {
        int startX = panel.getX() + 12, startY = panel.getY() + 10;
        var pressed = new ScreenEvent.MouseButtonPressed.Pre(screen, startX, startY, 0);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(pressed);
        check(pressed.isCanceled(), "Floating title consumes press");
        var dragged = new ScreenEvent.MouseDragged.Pre(screen, left + 12, top + 10, 0, left + 12 - startX, top + 10 - startY);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(dragged);
        check(dragged.isCanceled(), "Title continues dragging outside its original bounds");
        release(screen, left + 12, top + 10);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static void capture(String name) {
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) { image.writeToFile(Path.of("..", name + ".png")); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
}
