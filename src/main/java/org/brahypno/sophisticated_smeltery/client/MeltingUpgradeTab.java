package org.brahypno.sophisticated_smeltery.client;

import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiFuelModule;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiSmelteryTank;

public final class MeltingUpgradeTab extends UpgradeSettingsTab<MeltingUpgradeContainer> {
    private GuiSmelteryTank tankGui;
    private GuiFuelModule fuelGui;
    private Object displayedTank;

    public MeltingUpgradeTab(MeltingUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, container.getUpgradeWrapper().getFurnace().controller().getDisplayName(),
                container.getUpgradeStack().getHoverName());
        addHideableChild(new NativeTank(new Position(x + 5, y + 24)));
        addHideableChild(new NativeFuel(new Position(x + 115, y + 41)));
        addHideableChild(new BucketTransferModeButton(new Position(x + 82, y + 163),
                () -> container.getUpgradeWrapper().getTransferDirection(), () -> container.action("bucketMode")));
        for (int index = 0; index < Math.min(2, container.getUpgradeWrapper().getBackpackTanks().size()); index++) {
            int tank = index;
            addHideableChild(new TextButton(new Position(x + 5 + index * 56, y + 201),
                    () -> text("tank_button", tank + 1), button -> container.transfer(tank, button == 0),
                    () -> List.of(text("backpack_tank", tank + 1), text("transfer_hint"))));
        }
        openTabDimension = new Dimension(138, container.getUpgradeWrapper().getBackpackTanks().isEmpty() ? 200 : 224);
    }

    static Component text(String key, Object... args) { return Component.translatable("gui.sophisticatedsmeltery." + key, args); }

    private void updateNativeModules() {
        var wrapper = getContainer().getUpgradeWrapper();
        if (displayedTank == wrapper.getTank()) return;
        displayedTank = wrapper.getTank();
        tankGui = new GuiSmelteryTank(screen, wrapper.getTank(), x - screen.getGuiLeft() + 5,
                y - screen.getGuiTop() + 24, 106, 106,
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(wrapper.getFurnace().controller().getType()));
        fuelGui = new GuiFuelModule(screen, wrapper.getFurnace().fuel(), x - screen.getGuiLeft() + 115,
                y - screen.getGuiTop() + 41, 16, 88, x - screen.getGuiLeft() + 116,
                y - screen.getGuiTop() + 25, false, HeatingStructureScreen.BACKGROUND);
    }

    @Override protected void moveSlotsToTab() {
        var slots = getContainer().getSlots();
        for (int index = 0; index < 9; index++) {
            slots.get(index).x = x - screen.getGuiLeft() + 5 + index % 3 * 18;
            slots.get(index).y = y - screen.getGuiTop() + 139 + index / 3 * 18;
        }
        slots.get(9).x = x - screen.getGuiLeft() + 65;
        slots.get(9).y = y - screen.getGuiTop() + 139;
        slots.get(10).x = x - screen.getGuiLeft() + 101;
        slots.get(10).y = y - screen.getGuiTop() + 139;
    }

    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        if (getWidth() <= 125) {
            super.renderBg(graphics, minecraft, mouseX, mouseY);
        } else {
            // Skip the texture's entire left corner, keeping the attachment edge continuous.
            int half = getHeight() / 2;
            GuiHelper.renderTiledControlBackground(graphics, x, y, getWidth(), getHeight(),
                    134, 0, 122, 256, 0, 6, half, getHeight() - half);
            graphics.blit(GuiHelper.GUI_CONTROLS, x - 3, y, 128, 256 - getHeight(), 3, getHeight());
        }
        if (!getContainer().isOpen()) return;
        GuiHelper.renderSlotsBackground(graphics, x + 4, y + 138, 3, 3);
        BucketTransferModeButton.renderContainerRow(graphics, x + 65, y + 139,
                getContainer().getUpgradeWrapper().getContainers().getStackInSlot(0).isEmpty());
    }

    private final class NativeTank extends WidgetBase {
        private NativeTank(Position position) { super(position, new Dimension(106, 106)); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            updateNativeModules();
            graphics.fill(x - 1, y - 1, x + 107, y + 107, 0xff737373);
            graphics.fill(x, y, x + 106, y + 106, 0xff252525);
            graphics.flush();
            tankGui.renderFluids(graphics.pose());
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.pose().pushPose();
            graphics.pose().translate(screen.getGuiLeft(), screen.getGuiTop(), 0);
            tankGui.renderHighlight(graphics, mouseX, mouseY);
            graphics.pose().popPose();
        }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY) || button > 1) return false;
            updateNativeModules();
            getContainer().fluidClick(tankGui.getFluidClicked((int)mouseX - screen.getGuiLeft(), (int)mouseY - screen.getGuiTop()), button == 1);
            return true;
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            updateNativeModules();
            tankGui.drawTooltip(graphics, mouseX, mouseY);
        }
    }

    private final class NativeFuel extends WidgetBase {
        private NativeFuel(Position position) { super(position, new Dimension(16, 88)); }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            updateNativeModules();
            graphics.fill(x - 1, y - 1, x + 17, y + 89, 0xff737373);
            graphics.fill(x, y, x + 16, y + 88, 0xff252525);
            graphics.flush();
            fuelGui.draw(graphics);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY) || button > 1) return false;
            getContainer().fuelClick(button == 1);
            return true;
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            updateNativeModules();
            fuelGui.addTooltip(graphics, mouseX, mouseY, getContainer().getUpgradeWrapper().getFurnace().fuel().hasSources());
        }
    }

    private static final class TextButton extends WidgetBase {
        private final Supplier<Component> label;
        private final IntConsumer action;
        private final Supplier<List<Component>> tooltip;
        private TextButton(Position position, Supplier<Component> label, IntConsumer action, Supplier<List<Component>> tooltip) {
            super(position, new Dimension(54, 17)); this.label = label; this.action = action; this.tooltip = tooltip;
        }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            graphics.fill(x, y, x + getWidth(), y + getHeight(), isHovered ? 0xff858585 : 0xff5c5c5c);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.drawString(font, font.plainSubstrByWidth(label.get().getString(), getWidth() - 4), x + 2, y + 4, 0xffffffff, false);
        }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY) || button > 1) return false;
            action.accept(button); return true;
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY)) graphics.renderTooltip(font, tooltip.get(), Optional.empty(), mouseX, mouseY);
        }
    }
}
