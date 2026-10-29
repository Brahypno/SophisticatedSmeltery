package org.brahypno.sophisticated_smeltery.client;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.TextBox;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.brahypno.sophisticated_smeltery.ClientConfig;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeContainer;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;

/** Native layout picker, slot artwork and filters in a storage upgrade sidebar. */
public final class TinkerStationUpgradeTab extends UpgradeSettingsTab<TinkerStationUpgradeContainer> {
    public static final int LAYOUT_COLUMNS = 5;
    private static final int PICKER_TOP = 26;
    private final int layoutRows, inputTop, resultTop;
    private int firstRow;
    private final TextBox name;
    private final TinkerStationInformationPanel information;

    public TinkerStationUpgradeTab(TinkerStationUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, text(container.getUpgradeWrapper().isAdvanced() ? "tinkers_anvil" : "tinker_station"), container.getUpgradeStack().getHoverName());
        layoutRows = ClientConfig.TINKER_STATION_TOOL_ROWS.get();
        inputTop = PICKER_TOP + layoutRows * 18 + 2;
        // Keep the result below the five player equipment slots even with a short picker.
        resultTop = Math.max(inputTop + 36, PICKER_TOP + container.equipmentSlots().size() * 18 + 2);
        int nameTop = Math.max(inputTop + 89, resultTop + 35);
        openTabDimension = new Dimension(125, nameTop + 17);
        for (int index = 0; index < LAYOUT_COLUMNS * layoutRows; index++) addCrafting(new LayoutButton(
                new Position(x + 8 + index % LAYOUT_COLUMNS * 18, y + PICKER_TOP + index / LAYOUT_COLUMNS * 18), index));
        for (int slot = 0; slot < container.getUpgradeWrapper().inventory().getSlots(); slot++)
            addCrafting(new SlotHint(new Position(x, y), slot));
        name = addCrafting(new TextBox(new Position(x + 32, y + nameTop), new Dimension(82, 9)) {
            @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
                graphics.blit(TinkerStationGui.TINKER, x - 1, y - 2, 0, 244, 90, 12);
            }
            @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0 || !isEditable() || !isMouseOver(mouseX, mouseY)) return false;
                setFocused(true);
                return true;
            }
            @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
                if (isMouseOver(mouseX, mouseY)) graphics.renderTooltip(font, text("tinker_station_name"), mouseX, mouseY);
            }
        });
        name.setMaxLength(50);
        name.setBordered(false);
        name.setTextColor(0xffffffff);
        name.setTextColorUneditable(0xffffffff);
        name.setValueWithoutNotification(container.getUpgradeWrapper().itemName());
        name.setResponder(container::rename);
        var destination = screen.getMenu().getStorageWrapper().getStorageType().equals("backpack")
                ? net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions.SHIFT_CLICK_TARGET
                : net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageButtonDefinitions.SHIFT_CLICK_TARGET;
        addCrafting(new ToggleButton<>(new Position(x + 8, y + inputTop), destination,
                button -> { if (button == 0) container.toggleDestination(); }, () -> getContainer().getUpgradeWrapper().shiftIntoStorage()));
        addCrafting(new Status(new Position(x + 94, y + resultTop)));
        information = new TinkerStationInformationPanel(screen, container);
    }
    private <W extends WidgetBase> W addCrafting(W widget) {
        return addHideableChild(widget);
    }
    public TinkerStationInformationPanel information() { return information; }
    public int layoutRows() { return layoutRows; }
    private static Component text(String key, Object... args) { return Component.translatable("gui.sophisticated_smeltery." + key, args); }
    private int hiddenRows() { return Math.max(0, (getContainer().getUpgradeWrapper().layouts().size() + LAYOUT_COLUMNS - 1) / LAYOUT_COLUMNS - layoutRows); }
    private StationSlotLayout layoutAt(int index) {
        firstRow = Math.min(firstRow, hiddenRows());
        var layouts = getContainer().getUpgradeWrapper().layouts();
        int actual = firstRow * LAYOUT_COLUMNS + index;
        return actual < layouts.size() ? layouts.get(actual) : null;
    }
    private Position inputPosition(int index) {
        var wrapper = getContainer().getUpgradeWrapper();
        var layout = wrapper.layout();
        var slot = layout.getSlot(index);
        if (!slot.isHidden()) return new Position(x + 8 + slot.getX(), y + inputTop + slot.getY());
        if (wrapper.inventory().getStackInSlot(index).isEmpty()) return new Position(-2000, -2000);
        int filled = 0;
        for (int i = 0; i < index; i++) if (layout.getSlot(i).isHidden() && !wrapper.inventory().getStackInSlot(i).isEmpty()) filled++;
        return new Position(x + 32 + filled * 18, y + inputTop);
    }
    @Override protected void moveSlotsToTab() { positionSlots(); }
    private void positionSlots() {
        var slots = getContainer().getSlots();
        if (!getContainer().isOpen()) {
            slots.forEach(slot -> { slot.x = -2000; slot.y = -2000; });
            return;
        }
        for (int slot = 0; slot < getContainer().getUpgradeWrapper().inventory().getSlots(); slot++) {
            var pos = inputPosition(slot);
            slots.get(slot).x = pos.x() - screen.getGuiLeft(); slots.get(slot).y = pos.y() - screen.getGuiTop();
        }
        var output = getContainer().resultSlot();
        output.x = x - screen.getGuiLeft() + 99; output.y = y - screen.getGuiTop() + resultTop + 5;
        var equipment = getContainer().equipmentSlots();
        for (int index = 0; index < equipment.size(); index++) {
            equipment.get(index).x = x - screen.getGuiLeft() + 104;
            equipment.get(index).y = y - screen.getGuiTop() + 27 + index * 18;
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        super.renderBg(graphics, minecraft, mouseX, mouseY);
        if (!getContainer().isOpen()) { name.setFocused(false); return; }
        positionSlots();
        var wrapper = getContainer().getUpgradeWrapper();
        boolean canRename = !wrapper.result().isEmpty();
        name.setEditable(canRename);
        name.setVisible(canRename);
        if (!canRename) name.setFocused(false);
        if (!name.isFocused() && !name.getValue().equals(wrapper.itemName())) name.setValueWithoutNotification(wrapper.itemName());
        for (int slot = 0; slot < wrapper.inventory().getSlots(); slot++) {
            var pos = inputPosition(slot);
            if (pos.x() >= x) {
                RenderSystem.enableBlend();
                graphics.setColor(1, 1, 1, 0.28f);
                graphics.blit(TinkerStationGui.TINKER, pos.x() - 1, pos.y() - 1, 176, 0, 18, 18);
                graphics.setColor(1, 1, 1, 1);
                graphics.blit(TinkerStationGui.TINKER, pos.x() - 1, pos.y() - 1, 194, 0, 18, 18);
            }
        }
        for (var equipment : getContainer().equipmentSlots()) {
            graphics.setColor(1, 1, 1, 0.28f);
            graphics.blit(TinkerStationGui.TINKER, screen.getGuiLeft() + equipment.x - 1,
                    screen.getGuiTop() + equipment.y - 1, 176, 0, 18, 18);
            graphics.setColor(1, 1, 1, 1);
            graphics.blit(TinkerStationGui.TINKER, screen.getGuiLeft() + equipment.x - 1,
                    screen.getGuiTop() + equipment.y - 1, 194, 0, 18, 18);
        }
        graphics.blit(TinkerStationGui.TINKER, x + 94, y + resultTop, 109, 33, 26, 26);
        graphics.drawString(font, ">", x + 85, y + resultTop + 9, 0xff606060, false);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (getContainer().isOpen() && mouseX >= x + 7 && mouseX < x + 8 + LAYOUT_COLUMNS * 18
                && mouseY >= y + PICKER_TOP - 1 && mouseY < y + PICKER_TOP + layoutRows * 18 && hiddenRows() > 0) {
            firstRow = Math.max(0, Math.min(hiddenRows(), firstRow + (delta > 0 ? -1 : 1))); return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    private void drawPattern(GuiGraphics graphics, Pattern pattern, int left, int top) {
        graphics.blit(left, top, 100, 16, 16, Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(pattern.getTexture()));
    }
    private final class LayoutButton extends WidgetBase {
        private final int index;
        private LayoutButton(Position position, int index) { super(position, Dimension.SQUARE_18); this.index = index; }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            var layout = layoutAt(index);
            if (layout != null) TinkerStationGui.button(graphics, x, y, 18, getContainer().getUpgradeWrapper().isAdvanced(),
                    layout.getName().equals(getContainer().getUpgradeWrapper().layout().getName()), isHovered);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            var layout = layoutAt(index);
            if (layout == null) return;
            ItemStack stack = layout.getIcon().getValue(ItemStack.class);
            Pattern pattern = layout.getIcon().getValue(Pattern.class);
            if (stack != null) graphics.renderItem(stack, x + 1, y + 1);
            else if (pattern != null) drawPattern(graphics, pattern, x + 1, y + 1);
        }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            var layout = layoutAt(index);
            if (button != 0 || !isMouseOver(mouseX, mouseY) || layout == null) return false;
            getContainer().select(layout.getName()); return true;
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            var layout = layoutAt(index);
            if (isMouseOver(mouseX, mouseY) && layout != null) graphics.renderComponentTooltip(font, List.of(layout.getDisplayName(), layout.getDescription()), mouseX, mouseY);
        }
    }
    private final class SlotHint extends WidgetBase {
        private final int slot;
        private SlotHint(Position position, int slot) { super(position, new Dimension(16, 16)); this.slot = slot; }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) { setPosition(inputPosition(slot)); }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            var wrapper = getContainer().getUpgradeWrapper();
            var pattern = wrapper.layout().getSlot(slot).getIcon();
            if (wrapper.inventory().getStackInSlot(slot).isEmpty() && pattern != null) {
                graphics.setColor(1, 1, 1, 0.35f); drawPattern(graphics, pattern, x, y); graphics.setColor(1, 1, 1, 1);
            }
        }
    }
    private final class Status extends WidgetBase {
        private Status(Position position) { super(position, new Dimension(26, 26)); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) { }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            var error = getContainer().getUpgradeWrapper().error();
            if (error != null) graphics.drawCenteredString(font, "!", x + 13, y + 9, 0xff9e3030);
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY)) {
                var error = getContainer().getUpgradeWrapper().error();
                graphics.renderComponentTooltip(font, List.of(error == null ? text("tinker_station_help") : error), mouseX, mouseY);
            }
        }
    }
}
