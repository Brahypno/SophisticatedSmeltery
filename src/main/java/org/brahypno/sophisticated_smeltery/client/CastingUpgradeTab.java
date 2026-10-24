package org.brahypno.sophisticated_smeltery.client;

import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LockIconButton;
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
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;

public final class CastingUpgradeTab extends UpgradeSettingsTab<CastingUpgradeContainer> {
    private static final ElementScreen FLOW_ARROW = new ElementScreen(HeatingStructureScreen.BACKGROUND, 124, 87, 18, 14, 256, 256);
    public CastingUpgradeTab(CastingUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, text((container.getUpgradeWrapper().supportsAutomatic() ? "advanced_" : "")
                + (container.getUpgradeWrapper().casting().requiresCast() ? "scorched_casting" : "casting")), container.getUpgradeStack().getHoverName());
        addHideableChild(new ModeButton(new Position(x + 8, y + 54)));
        addHideableChild(new CastingDisplay(new Position(x + 49, y + 28)));
        addHideableChild(new Preview(new Position(x + 101, y + 57)));
        addHideableChild(new SlotHint(new Position(x + 7, y + 27), 0));
        addHideableChild(new SlotHint(new Position(x + 100, y + 27), 1));
        addHideableChild(new Action(new Position(x + 100, y + 74), () -> text("cast_pour"), "pour",
                () -> container.getUpgradeWrapper().isCastingEnabled() && hasFluidStorage() && !casting().isCooling()));
        if (container.getUpgradeWrapper().supportsAutomatic()) {
            addHideableChild(new Action(new Position(x + 54, y + 92),
                    () -> text(container.getUpgradeWrapper().isAutomatic() ? "cast_automatic_on" : "cast_automatic_off"), "automatic", () -> true));
        }
        addHideableChild(new Action(new Position(x + 8, y + 92), () -> text("cast_return"), "return",
                () -> hasFluidStorage() && casting().hasFluid() && !casting().isCooling()));
        addHideableChild(new Action(new Position(x + 100, y + 92),
                () -> text(container.getUpgradeWrapper().isOutputToStorage() ? "cast_output_storage" : "cast_output_slot"), "output", () -> true));
        if (container.getUpgradeWrapper().casting().requiresCast()) {
            addHideableChild(new CastLockButton(new Position(x + 27, y + 53)));
        }
        openTabDimension = new Dimension(128, 115);
    }

    private static Component text(String key, Object... args) { return MeltingUpgradeTab.text(key, args); }
    private PortableCasting casting() {
        var casting = getContainer().getUpgradeWrapper().casting();
        if (Minecraft.getInstance().level != null) casting.bind(Minecraft.getInstance().level);
        return casting;
    }
    private boolean hasFluidStorage() { return getContainer().getUpgradeWrapper().hasFluidStorage(); }

    @Override protected void moveSlotsToTab() {
        var slots = getContainer().getSlots();
        slots.get(0).x = x - screen.getGuiLeft() + 8;
        slots.get(0).y = y - screen.getGuiTop() + 28;
        slots.get(1).x = x - screen.getGuiLeft() + 101;
        slots.get(1).y = y - screen.getGuiTop() + 28;
    }
    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        if (getWidth() <= 125) {
            super.renderBg(graphics, minecraft, mouseX, mouseY);
        } else {
            // Exclude both the duplicate left border and its corner from the tiled interior.
            int half = getHeight() / 2;
            GuiHelper.renderTiledControlBackground(graphics, x, y, getWidth(), getHeight(),
                    134, 0, 122, 256, 0, 6, half, getHeight() - half);
            graphics.blit(GuiHelper.GUI_CONTROLS, x - 3, y, 128, 256 - getHeight(), 3, getHeight());
        }
        if (!getContainer().isOpen()) return;
        GuiHelper.renderSlotsBackground(graphics, x + 7, y + 27, 1, 1);
        GuiHelper.renderSlotsBackground(graphics, x + 100, y + 27, 1, 1);
        flowArrow(graphics, x + 28, y + 27);
        flowArrow(graphics, x + 81, y + 27);
    }

    private static void flowArrow(GuiGraphics graphics, int left, int top) {
        graphics.pose().pushPose();
        graphics.pose().translate(left, top + 18, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(-90));
        FLOW_ARROW.draw(graphics, 0, 0);
        graphics.pose().popPose();
    }

    private final class SlotHint extends WidgetBase {
        private final int slot;
        private SlotHint(Position position, int slot) { super(position, Dimension.SQUARE_18); this.slot = slot; }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) { }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY) && casting().inventory().getStackInSlot(slot).isEmpty())
                graphics.renderComponentTooltip(font, List.of(text(slot == 0 ? "cast_input" : "cast_output")), mouseX, mouseY);
        }
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
        private Action(Position position, Supplier<Component> label, String action, BooleanSupplier available) {
            super(position, Dimension.SQUARE_18);
            this.label = label;
            this.action = action;
            this.available = available;
        }
        @Override protected boolean enabled() { return available.getAsBoolean(); }
        @Override protected void click() { getContainer().action(action); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            GuiHelper.blit(graphics, x, y, enabled() && isHovered
                    ? GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND : GuiHelper.DEFAULT_BUTTON_BACKGROUND);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            switch (action) {
                case "pour" -> graphics.renderItem(new ItemStack(casting().requiresCast()
                        ? TinkerSmeltery.scorchedFaucet : TinkerSmeltery.searedFaucet), x + 1, y + 1);
                case "return" -> icon(graphics, 144, 0);
                case "automatic" -> icon(graphics, getContainer().getUpgradeWrapper().isAutomatic() ? 144 : 160, 80);
                case "output" -> icon(graphics, getContainer().getUpgradeWrapper().isOutputToStorage() ? 64 : 160, 48);
                default -> { }
            }
            if (!enabled()) graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x80000000);
        }
        private void icon(GuiGraphics graphics, int u, int v) { graphics.blit(GuiHelper.ICONS, x + 1, y + 1, u, v, 16, 16); }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (!isMouseOver(mouseX, mouseY)) return;
            List<Component> lines = new ArrayList<>();
            lines.add(label.get());
            if (!hasFluidStorage() && (action.equals("pour") || action.equals("return"))) lines.add(text("cast_no_controller"));
            if (action.equals("output")) lines.add(text("cast_output_hint"));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
    private final class CastLockButton extends Clickable {
        private final LockIconButton icon = new LockIconButton(0, 0, button -> {});
        private CastLockButton(Position position) { super(position, new Dimension(20, 20)); }
        @Override protected boolean enabled() {
            return !casting().lockedCast().isEmpty() || !casting().inventory().getStackInSlot(0).isEmpty();
        }
        @Override protected void click() { getContainer().action("lock"); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            icon.setX(x);
            icon.setY(y);
            icon.setLocked(!casting().lockedCast().isEmpty());
            icon.active = enabled();
            icon.render(graphics, mouseX, mouseY, 0);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (!isMouseOver(mouseX, mouseY)) return;
            List<Component> lines = new ArrayList<>();
            lines.add(text(casting().lockedCast().isEmpty() ? "cast_lock_off" : "cast_lock_on"));
            lines.add(text("cast_lock_hint"));
            if (!casting().lockedCast().isEmpty()) lines.add(casting().lockedCast().getHoverName());
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
    private final class ModeButton extends Clickable {
        private ModeButton(Position position) { super(position, Dimension.SQUARE_18); }
        @Override protected boolean enabled() { return !casting().hasFluid(); }
        @Override protected void click() { getContainer().action("mode"); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            GuiHelper.blit(graphics, x, y, enabled() && isHovered
                    ? GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND : GuiHelper.DEFAULT_BUTTON_BACKGROUND);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.renderItem(new ItemStack(casting().mode() == PortableCasting.Mode.TABLE
                    ? (casting().requiresCast() ? TinkerSmeltery.scorchedTable : TinkerSmeltery.searedTable)
                    : (casting().requiresCast() ? TinkerSmeltery.scorchedBasin : TinkerSmeltery.searedBasin)), x + 1, y + 1);
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
            FluidStack selected = minecraft.level == null ? FluidStack.EMPTY : getContainer().getUpgradeWrapper().previewFluid(minecraft.level);
            preview = minecraft.level == null ? ItemStack.EMPTY : casting().preview(minecraft.level, selected);
            if (!preview.isEmpty()) graphics.renderFakeItem(preview, x, y);
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY) && !preview.isEmpty())
                graphics.renderComponentTooltip(font, List.of(text("cast_preview"), preview.getHoverName()), mouseX, mouseY);
        }
    }
}
