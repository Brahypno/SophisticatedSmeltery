package org.brahypno.sophisticated_smeltery.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.brahypno.sophisticated_smeltery.casting.PortableCasting;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeContainer;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

public final class CastingUpgradeTab extends UpgradeSettingsTab<CastingUpgradeContainer> {
    public CastingUpgradeTab(CastingUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, text("casting"), container.getUpgradeStack().getHoverName());
        addHideableChild(new ModeButton(new Position(x + 8, y + 54)));
        addHideableChild(new CastingDisplay(new Position(x + 49, y + 28)));
        addHideableChild(new Preview(new Position(x + 101, y + 57)));
        addHideableChild(new Action(new Position(x + 5, y + 92), 37, () -> text("cast_pour"), "pour",
                () -> hasController() && !casting().isCooling() && casting().inventory().getStackInSlot(1).isEmpty()));
        addHideableChild(new Action(new Position(x + 45, y + 92), 78,
                () -> text(container.getUpgradeWrapper().isAutomatic() ? "cast_automatic_on" : "cast_automatic_off"), "automatic", () -> true));
        addHideableChild(new Action(new Position(x + 5, y + 113), 118, () -> text("cast_return"), "return",
                () -> hasController() && casting().hasFluid() && !casting().isCooling()));
        openTabDimension = new Dimension(128, 138);
    }

    private static Component text(String key, Object... args) { return MeltingUpgradeTab.text(key, args); }
    private PortableCasting casting() {
        var casting = getContainer().getUpgradeWrapper().casting();
        if (Minecraft.getInstance().level != null) casting.bind(Minecraft.getInstance().level);
        return casting;
    }
    private boolean hasController() { return getContainer().getUpgradeWrapper().controller().isPresent(); }

    @Override protected void moveSlotsToTab() {
        var slots = getContainer().getSlots();
        slots.get(0).x = x - screen.getGuiLeft() + 8;
        slots.get(0).y = y - screen.getGuiTop() + 28;
        slots.get(1).x = x - screen.getGuiLeft() + 101;
        slots.get(1).y = y - screen.getGuiTop() + 28;
    }
    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        super.renderBg(graphics, minecraft, mouseX, mouseY);
        if (!getContainer().isOpen()) return;
        GuiHelper.renderSlotsBackground(graphics, x + 7, y + 27, 1, 1);
        GuiHelper.renderSlotsBackground(graphics, x + 100, y + 27, 1, 1);
    }

    private abstract class Clickable extends WidgetBase {
        private Clickable(Position position, Dimension dimension) { super(position, dimension); }
        protected abstract void click();
        protected boolean enabled() { return true; }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !isMouseOver(mouseX, mouseY)) return false;
            if (enabled()) {
                click();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
            }
            return true;
        }
    }
    private final class Action extends Clickable {
        private final Supplier<Component> label;
        private final String action;
        private final BooleanSupplier available;
        private Action(Position position, int width, Supplier<Component> label, String action, BooleanSupplier available) {
            super(position, new Dimension(width, 18));
            this.label = label;
            this.action = action;
            this.available = available;
        }
        @Override protected boolean enabled() { return available.getAsBoolean(); }
        @Override protected void click() { getContainer().action(action); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            graphics.fill(x, y, x + getWidth(), y + 18, enabled() && isHovered ? 0xff858585 : 0xff5c5c5c);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            String label = font.plainSubstrByWidth(this.label.get().getString(), getWidth() - 4);
            graphics.drawString(font, label, x + (getWidth() - font.width(label)) / 2, y + 5,
                    enabled() ? 0xffffffff : 0xffa0a0a0, false);
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY) && !hasController() && !action.equals("automatic"))
                graphics.renderTooltip(font, text("cast_no_controller"), mouseX, mouseY);
        }
    }
    private final class ModeButton extends Clickable {
        private ModeButton(Position position) { super(position, Dimension.SQUARE_18); }
        @Override protected boolean enabled() { return !casting().hasFluid(); }
        @Override protected void click() { getContainer().action("mode"); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            GuiHelper.renderSlotsBackground(graphics, x, y, 1, 1);
            if (isHovered && enabled()) graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x40ffffff);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.renderItem(new ItemStack(casting().mode() == PortableCasting.Mode.TABLE ? TinkerSmeltery.searedTable : TinkerSmeltery.searedBasin), x + 1, y + 1);
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (!isMouseOver(mouseX, mouseY)) return;
            List<Component> lines = new ArrayList<>();
            lines.add(text(casting().mode() == PortableCasting.Mode.TABLE ? "cast_table" : "cast_basin"));
            if (!enabled()) lines.add(text("cast_mode_locked"));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
    private final class CastingDisplay extends WidgetBase {
        private CastingDisplay(Position position) { super(position, new Dimension(26, 54)); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            var casting = casting();
            graphics.fill(x - 1, y - 1, x + 17, y + 55, 0xff737373);
            graphics.fill(x, y, x + 16, y + 54, 0xff252525);
            FluidStack fluid = casting.fluid();
            if (!fluid.isEmpty()) {
                int height = Math.max(3, (int)(54L * fluid.getAmount() / Math.max(fluid.getAmount(), casting.required())));
                graphics.flush();
                GuiUtil.renderTiledFluid(graphics.pose(), CastingUpgradeTab.this.screen, fluid,
                        x - CastingUpgradeTab.this.screen.getGuiLeft(), y - CastingUpgradeTab.this.screen.getGuiTop() + 54 - height, 16, height, 100);
            }
            graphics.fill(x + 22, y, x + 26, y + 54, 0xff373737);
            int height = casting.coolingTime() <= 0 ? 0 : Math.min(54, 54 * casting.timer() / casting.coolingTime());
            if (height > 0) graphics.fill(x + 22, y + 54 - height, x + 26, y + 54, 0xff86c6e8);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (!isMouseOver(mouseX, mouseY)) return;
            var casting = casting();
            List<Component> lines = new ArrayList<>();
            if (mouseX < x + 17 && casting.hasFluid()) lines.addAll(FluidTooltipHandler.getFluidTooltip(casting.fluid()));
            if (casting.isCooling()) lines.add(text("cast_cooling", casting.timer(), casting.coolingTime()));
            else if (casting.hasFluid()) lines.add(text("cast_waiting"));
            if (!lines.isEmpty()) graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
    private final class Preview extends WidgetBase {
        private ItemStack preview = ItemStack.EMPTY;
        private Preview(Position position) { super(position, new Dimension(16, 16)); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) { }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            FluidStack selected = getContainer().getUpgradeWrapper().controller()
                    .map(controller -> controller.getTank().getFluidInTank(0)).orElse(FluidStack.EMPTY);
            preview = minecraft.level == null ? ItemStack.EMPTY : casting().preview(minecraft.level, selected);
            if (!preview.isEmpty()) graphics.renderFakeItem(preview, x, y);
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY) && !preview.isEmpty())
                graphics.renderComponentTooltip(font, List.of(text("cast_preview"), preview.getHoverName()), mouseX, mouseY);
        }
    }
}
