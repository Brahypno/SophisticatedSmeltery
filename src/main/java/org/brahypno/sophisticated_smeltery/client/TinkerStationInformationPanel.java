package org.brahypno.sophisticated_smeltery.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.upgrade.ModifierWorktableUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeContainer;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.item.ITinkerStationDisplay;
import slimeknights.tconstruct.tables.client.inventory.widget.BorderWidget;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;

/** Both native information panels, stacked and draggable beside the storage screen. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class TinkerStationInformationPanel extends WidgetBase {
    private static final int WIDTH = 166, STATS_HEIGHT = 134, GAP = 4, MODIFIERS_HEIGHT = 106;
    private static final int HEIGHT = STATS_HEIGHT + GAP + MODIFIERS_HEIGHT, BODY_TOP = 18;
    private static final Component ASCII_ANVIL = Component.literal("\n\n       .\n     /( _________\n     |  >:=========`\n     )(  \n     \"\"")
            .withStyle(ChatFormatting.DARK_GRAY);
    private record Line(FormattedCharSequence text, Component tooltip) {}
    private final StorageScreenBase<?> screen;
    private final UpgradeContainerBase<?, ?> container;
    private final Section stats, modifiers;
    private ItemStack displayed = ItemStack.EMPTY;
    private boolean shift, control, positioned, dragging, toolInformation;
    private Section scrolling;
    private double grabX, grabY;

    TinkerStationInformationPanel(StorageScreenBase<?> screen, UpgradeContainerBase<?, ?> container) {
        super(new Position(0, 0), new Dimension(WIDTH, HEIGHT));
        this.screen = screen;
        this.container = container;
        stats = new Section(STATS_HEIGHT, 8 / 9f);
        modifiers = new Section(MODIFIERS_HEIGHT, 7 / 9f);
    }
    public Section stats() { return stats; }
    public Section modifiers() { return modifiers; }
    private static Component text(String key) { return Component.translatable("gui.sophisticated_smeltery." + key); }
    private boolean active() { return container.isOpen(); }
    private boolean advanced() {
        return container instanceof TinkerStationUpgradeContainer station && station.getUpgradeWrapper().isAdvanced();
    }
    private void position(int left, int top) {
        setPosition(new Position(Math.max(0, Math.min(screen.width - WIDTH, left)),
                Math.max(0, Math.min(screen.height - HEIGHT, top))));
        stats.setPosition(new Position(x, y));
        modifiers.setPosition(new Position(x, y + STATS_HEIGHT + GAP));
    }
    private void refresh() {
        if (container instanceof ModifierWorktableUpgradeContainer worktable) {
            refreshWorktable(worktable); return;
        }
        var wrapper = ((TinkerStationUpgradeContainer)container).getUpgradeWrapper();
        ItemStack stack = TinkerStationToolInfo.displayedTool(wrapper);
        if (stack.is(TinkerTags.Items.MODIFIABLE)) {
            showToolInformation(stack);
        } else {
            toolInformation = false;
            // The native station replaces tool data with layout instructions and required components.
            var layout = wrapper.layout();
            stats.setContent(layout.getDisplayName(), List.of(new TinkerStationToolInfo.Entry(layout.getDescription())));
            List<TinkerStationToolInfo.Entry> components = new ArrayList<>();
            int inputs = Math.min(layout.getInputCount(), wrapper.inventory().getSlots() - 1);
            for (int index = 0; index <= inputs; index++) {
                var slot = layout.getSlot(index);
                if (slot.isHidden() || slot.getTranslationKey().isEmpty()) continue;
                var label = Component.literal(" * ");
                if (!slot.isValid(wrapper.inventory().getStackInSlot(index))) label.withStyle(ChatFormatting.RED);
                label.append(Component.translatable(slot.getTranslationKey()));
                components.add(new TinkerStationToolInfo.Entry(label));
            }
            modifiers.setContent(components.isEmpty() ? Component.empty() : Component.translatable("gui.tconstruct.tinker_station.components"),
                    components.isEmpty() ? List.of(new TinkerStationToolInfo.Entry(ASCII_ANVIL)) : components);
        }
    }
    /** Follows ModifierWorktableScreen.updateDisplay: messages take precedence over result data. */
    private void refreshWorktable(ModifierWorktableUpgradeContainer worktable) {
        var wrapper = worktable.getUpgradeWrapper();
        Component message = wrapper.message();
        ItemStack result = wrapper.result();
        if (!message.getString().isEmpty() || result.isEmpty()) {
            toolInformation = false;
            var recipe = wrapper.recipe();
            stats.setContent(recipe == null ? Component.translatable("gui.tconstruct.modifier_worktable.title") : recipe.getTitle(),
                    List.of(new TinkerStationToolInfo.Entry(message.getString().isEmpty()
                            ? Component.translatable("gui.tconstruct.modifier_worktable.info") : message)));
            modifiers.setContent(Component.empty(), List.of());
        } else showToolInformation(result);
    }
    private void showToolInformation(ItemStack stack) {
        boolean currentShift = Screen.hasShiftDown(), currentControl = Screen.hasControlDown();
        if (toolInformation && ItemStack.matches(stack, displayed) && currentShift == shift && currentControl == control) return;
        displayed = stack.copy(); shift = currentShift; control = currentControl;
        toolInformation = stack.is(TinkerTags.Items.MODIFIABLE);
        Component caption = stack.getItem() instanceof ITinkerStationDisplay display ? display.getLocalizedName() : stack.getHoverName();
        stats.setContent(caption, TinkerStationToolInfo.stats(stack, minecraft.player));
        if (toolInformation) {
            modifiers.setContent(Component.translatable("gui.tconstruct.tinker_station." + (control ? "traits" : shift ? "upgrades" : "modifiers")),
                    TinkerStationToolInfo.modifiers(stack, minecraft.player, shift, control));
        } else {
            var modifierId = ModifierCrystalItem.getModifier(stack);
            if (modifierId == null) modifiers.setContent(Component.empty(), List.of());
            else {
                var modifier = ModifierManager.getValue(modifierId);
                modifiers.setContent(Component.translatable("gui.tconstruct.tinker_station.modifiers"),
                        List.of(new TinkerStationToolInfo.Entry(modifier.getDisplayName(), modifier.getDescription())));
            }
        }
    }
    @Override public boolean isMouseOver(double mouseX, double mouseY) { return active() && super.isMouseOver(mouseX, mouseY); }
    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        refresh();
        stats.render(graphics, mouseX, mouseY, 0);
        modifiers.render(graphics, mouseX, mouseY, 0);
    }
    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {}
    private Section sectionAt(double mouseX, double mouseY) {
        return stats.isMouseOver(mouseX, mouseY) ? stats : modifiers.isMouseOver(mouseX, mouseY) ? modifiers : null;
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (button == 0) {
            refresh();
            var section = sectionAt(mouseX, mouseY);
            if (section != null && mouseY < section.getY() + BODY_TOP) {
                dragging = true; grabX = mouseX - x; grabY = mouseY - y;
            } else if (section != null && section.maxOffset() > 0 && mouseX >= x + WIDTH - 11) {
                scrolling = section;
                section.scrollTo(mouseY);
            }
        }
        return true;
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!active() || button != 0) return false;
        if (scrolling != null) { scrolling.scrollTo(mouseY); return true; }
        if (!dragging) return false;
        position((int)(mouseX - grabX), (int)(mouseY - grabY));
        return true;
    }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = dragging || scrolling != null || isMouseOver(mouseX, mouseY);
        if (button == 0) { dragging = false; scrolling = null; }
        return handled;
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        refresh();
        var section = sectionAt(mouseX, mouseY);
        if (section != null) section.offset = Math.max(0, Math.min(section.maxOffset(), section.offset + (delta > 0 ? -3 : 3)));
        return true;
    }
    @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        var section = sectionAt(mouseX, mouseY);
        if (section != null) section.renderTooltip(screen, graphics, mouseX, mouseY);
    }

    public final class Section extends WidgetBase {
        private final BorderWidget border;
        private final float scale, lineHeight;
        private final int rows;
        private Component caption = Component.empty();
        private List<TinkerStationToolInfo.Entry> entries = List.of();
        private List<Line> lines = List.of();
        private int offset;

        private Section(int height, float scale) {
            super(new Position(0, 0), new Dimension(WIDTH, height));
            this.scale = scale;
            lineHeight = (font.lineHeight + 0.5f) * scale;
            rows = (int)((height - BODY_TOP - 5) / lineHeight);
            border = TinkerStationGui.border(advanced());
        }
        public Component caption() { return caption; }
        public List<TinkerStationToolInfo.Entry> entries() { return entries; }
        public int offset() { return offset; }
        private int maxOffset() { return Math.max(0, lines.size() - rows); }
        private void setContent(Component caption, List<TinkerStationToolInfo.Entry> entries) {
            if (this.caption.equals(caption) && this.entries.equals(entries)) return;
            this.caption = caption;
            this.entries = List.copyOf(entries);
            List<Line> wrapped = new ArrayList<>();
            for (var entry : entries) for (var line : font.split(entry.text(), (int)((WIDTH - 19) / scale)))
                wrapped.add(new Line(line, entry.tooltip()));
            lines = wrapped;
            offset = Math.min(offset, maxOffset());
        }
        private void scrollTo(double mouseY) {
            int trackHeight = getHeight() - BODY_TOP - 5;
            offset = Math.max(0, Math.min(maxOffset(), (int)Math.round((mouseY - y - BODY_TOP - 2.5) * maxOffset() / (trackHeight - 5))));
        }
        @Override public boolean isMouseOver(double mouseX, double mouseY) { return active() && super.isMouseOver(mouseX, mouseY); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            TinkerStationGui.panel(graphics, border, x, y, WIDTH, getHeight());
            if (maxOffset() > 0) TinkerStationGui.scrollbar(graphics, x + WIDTH - 8, y + BODY_TOP,
                    getHeight() - BODY_TOP - 5, offset, maxOffset(), advanced(),
                    mouseX >= x + WIDTH - 11 && isMouseOver(mouseX, mouseY));
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.drawCenteredString(font, caption.copy().withStyle(ChatFormatting.UNDERLINE), x + WIDTH / 2, y + 5, 0xfff0f0f0);
            graphics.enableScissor(x + 5, y + BODY_TOP, x + WIDTH - 12, y + getHeight() - 5);
            graphics.pose().pushPose();
            graphics.pose().translate(x + 6, y + BODY_TOP, 0);
            graphics.pose().scale(scale, scale, 1);
            for (int row = 0; row < rows && offset + row < lines.size(); row++)
                graphics.drawString(font, lines.get(offset + row).text(), 0, (int)(row * (font.lineHeight + 0.5f)), 0xfff0f0f0, true);
            graphics.pose().popPose();
            graphics.disableScissor();
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (!isMouseOver(mouseX, mouseY)) return;
            if (mouseY < y + BODY_TOP) {
                graphics.renderComponentTooltip(font, this == modifiers && toolInformation
                        ? List.of(text("tinker_station_drag_hint"), text("tinker_station_modifiers_hint"))
                        : List.of(text("tinker_station_drag_hint")), mouseX, mouseY);
                return;
            }
            int row = (int)((mouseY - y - BODY_TOP) / lineHeight);
            if (row >= 0 && row < rows && offset + row < lines.size()) {
                var tooltip = lines.get(offset + row).tooltip();
                if (tooltip != null) graphics.renderComponentTooltip(font, List.of(tooltip), mouseX, mouseY);
            }
        }
    }

    private static TinkerStationInformationPanel panel(Screen screen) {
        if (!(screen instanceof StorageScreenBase<?> storage)) return null;
        return storage.getUpgradeSettingsControl().children().stream().map(tab -> {
                    if (tab instanceof TinkerStationUpgradeTab station) return station.information();
                    if (tab instanceof ModifierWorktableUpgradeTab worktable) return worktable.information();
                    return null;
                }).filter(panel -> panel != null && panel.active()).findFirst().orElse(null);
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        var panel = panel(event.getScreen());
        if (panel == null) return;
        if (!panel.positioned) {
            panel.position(panel.screen.getGuiLeft() - WIDTH - 30, panel.screen.getGuiTop());
            panel.positioned = true;
        }
        var graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        panel.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        panel.renderTooltip(event.getScreen(), graphics, event.getMouseX(), event.getMouseY());
        graphics.pose().popPose();
    }
    // Handle the floating window before the storage screen or JEI can use the same input.
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        var panel = panel(event.getScreen());
        if (panel != null && panel.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void drag(ScreenEvent.MouseDragged.Pre event) {
        var panel = panel(event.getScreen());
        if (panel != null && panel.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        var panel = panel(event.getScreen());
        if (panel != null && panel.mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        var panel = panel(event.getScreen());
        if (panel != null && panel.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDelta())) event.setCanceled(true);
    }
}
