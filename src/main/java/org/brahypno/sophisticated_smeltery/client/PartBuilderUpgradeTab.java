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
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.brahypno.sophisticated_smeltery.upgrade.PartBuilderUpgradeContainer;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.utils.Util;

/** The familiar upgrade sidebar with Tinkers' native four-by-four pattern picker. */
public final class PartBuilderUpgradeTab extends UpgradeSettingsTab<PartBuilderUpgradeContainer> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("tconstruct", "textures/gui/part_builder.png");
    private float scrollProgress;

    public PartBuilderUpgradeTab(PartBuilderUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, text("part_builder"), container.getUpgradeStack().getHoverName());
        openTabDimension = new Dimension(125, 170);
        for (int index = 0; index < 16; index++)
            addHideableChild(new PatternButton(new Position(x + 8 + index % 4 * 18, y + 26 + index / 4 * 18), index));
        addHideableChild(new PatternScrollBar(new Position(x + 83, y + 26)));
        addHideableChild(new InputHint(new Position(x + 9, y + 106), 0));
        addHideableChild(new InputHint(new Position(x + 33, y + 106), 1));
        var destination = screen.getMenu().getStorageWrapper().getStorageType().equals("backpack")
                ? net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions.SHIFT_CLICK_TARGET
                : net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageButtonDefinitions.SHIFT_CLICK_TARGET;
        addHideableChild(new ToggleButton<>(new Position(x + 8, y + 145), destination,
                button -> { if (button == 0) container.toggleDestination(); }, () -> getContainer().getUpgradeWrapper().shiftIntoStorage()));
    }
    private static Component text(String key, Object... args) { return Component.translatable("gui.sophisticated_smeltery." + key, args); }
    private int hiddenRows() { return Math.max(0, (getContainer().getUpgradeWrapper().patterns().size() + 3) / 4 - 4); }
    private void clampScroll() { if (hiddenRows() == 0) scrollProgress = 0; }
    private Pattern patternAt(int index) {
        clampScroll();
        var patterns = getContainer().getUpgradeWrapper().patterns();
        int actual = Math.round(scrollProgress * hiddenRows()) * 4 + index;
        return actual < patterns.size() ? patterns.get(actual) : null;
    }
    @Override protected void moveSlotsToTab() {
        int[] left = {9, 33, 93};
        for (int slot = 0; slot < 3; slot++) {
            getContainer().getSlots().get(slot).x = x - screen.getGuiLeft() + left[slot];
            getContainer().getSlots().get(slot).y = y - screen.getGuiTop() + 106;
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        super.renderBg(graphics, minecraft, mouseX, mouseY);
        if (!getContainer().isOpen()) return;
        graphics.blit(TEXTURE, x + 7, y + 25, 50, 14, 74, 74);
        graphics.blit(TEXTURE, x + 8, y + 105, 7, 42, 18, 18);
        graphics.blit(TEXTURE, x + 32, y + 105, 28, 42, 18, 18);
        graphics.blit(TEXTURE, x + 88, y + 101, 143, 37, 26, 26);
        graphics.blit(new ResourceLocation("minecraft", "textures/gui/container/crafting_table.png"), x + 60, y + 107, 89, 35, 22, 16);
        var wrapper = getContainer().getUpgradeWrapper();
        var material = wrapper.getMaterial();
        var recipe = wrapper.selectedRecipe();
        String value = material == null ? "0" : Util.COMMA_FORMAT.format(material.getMaterialValue(wrapper));
        Component cost = recipe == null ? text("part_builder_select") : text("part_builder_cost", value, recipe.getCost());
        graphics.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(cost, 109)), x + 8, y + 130,
                recipe != null && wrapper.result().isEmpty() ? 0xff9e3030 : 0xff404040, false);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (getContainer().isOpen() && mouseX >= x + 7 && mouseX < x + 96 && mouseY >= y + 25 && mouseY < y + 99 && hiddenRows() > 0) {
            scrollProgress = Mth.clamp((float)(scrollProgress - delta / hiddenRows()), 0, 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    private final class PatternButton extends WidgetBase {
        private final int index;
        private PatternButton(Position position, int index) { super(position, Dimension.SQUARE_18); this.index = index; }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            Pattern pattern = patternAt(index);
            if (pattern == null) return;
            int v = pattern.equals(getContainer().getUpgradeWrapper().selected()) ? 33 : isHovered ? 51 : 15;
            graphics.blit(TEXTURE, x, y, 176, v, 18, 18);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            Pattern pattern = patternAt(index);
            if (pattern != null) graphics.blit(x + 1, y + 1, 100, 16, 16,
                    Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(pattern.getTexture()));
        }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            Pattern pattern = patternAt(index);
            if (button != 0 || !isMouseOver(mouseX, mouseY) || pattern == null) return false;
            getContainer().select(pattern);
            return true;
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            Pattern pattern = patternAt(index);
            if (isMouseOver(mouseX, mouseY) && pattern != null) {
                var recipe = getContainer().getUpgradeWrapper().recipe(pattern);
                graphics.renderComponentTooltip(font, List.of(pattern.getDisplayName(),
                        text("part_builder_cost", getContainer().getUpgradeWrapper().getMaterial() == null ? "0"
                                : Util.COMMA_FORMAT.format(getContainer().getUpgradeWrapper().getMaterial().getMaterialValue(getContainer().getUpgradeWrapper())), recipe.getCost())), mouseX, mouseY);
            }
        }
    }
    private final class PatternScrollBar extends WidgetBase {
        private static final int HANDLE_HEIGHT = 15;
        private PatternScrollBar(Position position) { super(position, new Dimension(12, 72)); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            clampScroll();
            graphics.blit(TEXTURE, x - 1, y - 1, 125, 14, 14, 74);
            graphics.blit(TEXTURE, x, y + Math.round(scrollProgress * (getHeight() - HANDLE_HEIGHT)),
                    hiddenRows() > 0 ? 176 : 188, 0, 12, HANDLE_HEIGHT);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }
        private void scrollTo(double mouseY) {
            scrollProgress = Mth.clamp((float)((mouseY - y - HANDLE_HEIGHT / 2.0) / (getHeight() - HANDLE_HEIGHT)), 0, 1);
        }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !isMouseOver(mouseX, mouseY) || hiddenRows() == 0) return false;
            scrollTo(mouseY);
            return true;
        }
        @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button != 0 || hiddenRows() == 0) return false;
            scrollTo(mouseY);
            return true;
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY)) graphics.renderTooltip(font, text("part_builder_scroll"), mouseX, mouseY);
        }
    }
    private final class InputHint extends WidgetBase {
        private final int slot;
        private InputHint(Position position, int slot) { super(position, new Dimension(16, 16)); this.slot = slot; }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) { }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (getContainer().getSlots().get(slot).getItem().isEmpty()) {
                // Native hints include the slot border: draw their 18x18 area around the 16x16 item.
                (slot == 0 ? slimeknights.tconstruct.library.client.Icons.PATTERN : slimeknights.tconstruct.library.client.Icons.INGOT).draw(graphics, x - 1, y - 1);
            }
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY) && getContainer().getSlots().get(slot).getItem().isEmpty())
                graphics.renderTooltip(font, text(slot == 0 ? "part_builder_pattern" : "part_builder_material"), mouseX, mouseY);
        }
    }
}
