package org.brahypno.sophisticated_smeltery.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.brahypno.sophisticated_smeltery.upgrade.ModifierWorktableUpgradeContainer;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

/** Native worktable icons and operations within the host's upgrade sidebar. */
public final class ModifierWorktableUpgradeTab extends UpgradeSettingsTab<ModifierWorktableUpgradeContainer> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("tconstruct", "textures/gui/worktable.png");
    private static final ResourceLocation CRAFTING_TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/crafting_table.png");
    private static final List<Pattern> HINTS = List.of(new Pattern("tconstruct:pickaxe"), new Pattern("tconstruct:ingot"), new Pattern("tconstruct:quartz"));
    private final TinkerStationInformationPanel information;
    private float scrollProgress;
    public ModifierWorktableUpgradeTab(ModifierWorktableUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, Component.translatable("gui.tconstruct.modifier_worktable"), container.getUpgradeStack().getHoverName());
        openTabDimension = new Dimension(125, 175);
        for (int index = 0; index < 16; index++) addHideableChild(new ModifierButton(new Position(x + 8 + index % 4 * 18, y + 26 + index / 4 * 18), index));
        addHideableChild(new ModifierScrollBar(new Position(x + 83, y + 26)));
        for (int slot = 0; slot < 3; slot++) addHideableChild(new InputHint(new Position(x + 9 + slot * 22, y + 124), slot));
        var destination = screen.getMenu().getStorageWrapper().getStorageType().equals("backpack")
                ? net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions.SHIFT_CLICK_TARGET
                : net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageButtonDefinitions.SHIFT_CLICK_TARGET;
        addHideableChild(new ToggleButton<>(new Position(x + 8, y + 151), destination,
                button -> { if (button == 0) container.toggleDestination(); }, () -> getContainer().getUpgradeWrapper().shiftIntoStorage()));
        information = new TinkerStationInformationPanel(screen, container);
    }
    public TinkerStationInformationPanel information() { return information; }
    private int hiddenRows() { return Math.max(0, (getContainer().getUpgradeWrapper().options().size() + 3) / 4 - 4); }
    private int firstIndex() {
        if (hiddenRows() == 0) scrollProgress = 0;
        return Math.round(scrollProgress * hiddenRows()) * 4;
    }
    private ModifierEntry option(int index) {
        var options = getContainer().getUpgradeWrapper().options();
        int actual = firstIndex() + index;
        return actual < options.size() ? options.get(actual) : null;
    }
    @Override protected void moveSlotsToTab() {
        for (int slot = 0; slot < 4; slot++) {
            getContainer().getSlots().get(slot).x = x - screen.getGuiLeft() + (slot == 3 ? 99 : 9 + slot * 22);
            getContainer().getSlots().get(slot).y = y - screen.getGuiTop() + 124;
        }
        for (int index = 0; index < getContainer().equipmentSlots().size(); index++) {
            var slot = getContainer().equipmentSlots().get(index);
            slot.x = x - screen.getGuiLeft() + 104; slot.y = y - screen.getGuiTop() + 27 + index * 18;
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        super.renderBg(graphics, minecraft, mouseX, mouseY);
        if (!getContainer().isOpen()) return;
        graphics.blit(TEXTURE, x + 7, y + 25, 27, 14, 74, 74);
        for (int slot = 0; slot < 3; slot++) graphics.blit(TEXTURE, x + 8 + slot * 22, y + 123, 7, 20, 18, 18);
        graphics.blit(TEXTURE, x + 94, y + 119, 120, 37, 26, 26);
        graphics.blit(CRAFTING_TEXTURE, x + 71, y + 124, 89, 35, 22, 16);
        for (var slot : getContainer().equipmentSlots()) graphics.blit(TEXTURE, screen.getGuiLeft() + slot.x - 1, screen.getGuiTop() + slot.y - 1, 7, 20, 18, 18);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (getContainer().isOpen() && mouseX >= x + 7 && mouseX < x + 96 && mouseY >= y + 25 && mouseY < y + 99 && hiddenRows() > 0) {
            scrollProgress = Mth.clamp((float)(scrollProgress - delta / hiddenRows()), 0, 1); return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    private final class ModifierButton extends WidgetBase {
        private final int index;
        private ModifierButton(Position position, int index) { super(position, Dimension.SQUARE_18); this.index = index; }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            if (option(index) != null) graphics.blit(TEXTURE, x, y, 176, firstIndex() + index == getContainer().getUpgradeWrapper().selected() ? 33 : isHovered ? 51 : 15, 18, 18);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            var entry = option(index);
            if (entry != null) ModifierIconManager.renderIcon(graphics, entry.getModifier(), x + 1, y + 1, 100, 16);
        }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !isMouseOver(mouseX, mouseY) || option(index) == null) return false;
            getContainer().select(firstIndex() + index); return true;
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            var entry = option(index);
            if (isMouseOver(mouseX, mouseY) && entry != null) graphics.renderTooltip(font, entry.getDisplayName(), mouseX, mouseY);
        }
    }
    private final class ModifierScrollBar extends WidgetBase {
        private ModifierScrollBar(Position position) { super(position, new Dimension(12, 72)); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            firstIndex();
            graphics.blit(TEXTURE, x - 1, y - 1, 102, 14, 14, 74);
            graphics.blit(TEXTURE, x, y + Math.round(scrollProgress * 57), hiddenRows() > 0 ? 176 : 188, 0, 12, 15);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }
        private void scrollTo(double mouseY) { scrollProgress = Mth.clamp((float)((mouseY - y - 7.5) / 57), 0, 1); }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !isMouseOver(mouseX, mouseY) || hiddenRows() == 0) return false;
            scrollTo(mouseY); return true;
        }
        @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button != 0 || hiddenRows() == 0) return false;
            scrollTo(mouseY); return true;
        }
    }
    private final class InputHint extends WidgetBase {
        private final int slot;
        private InputHint(Position position, int slot) { super(position, new Dimension(16, 16)); this.slot = slot; }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) { }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (getContainer().getSlots().get(slot).getItem().isEmpty()) graphics.blit(x, y, 100, 16, 16, Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(HINTS.get(slot).getTexture()));
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY) && getContainer().getSlots().get(slot).getItem().isEmpty())
                graphics.renderTooltip(font, Component.translatable("gui.sophisticated_smeltery." + (slot == 0 ? "tinker_station_tool" : "tinker_station_input"), slot), mouseX, mouseY);
        }
    }
}
