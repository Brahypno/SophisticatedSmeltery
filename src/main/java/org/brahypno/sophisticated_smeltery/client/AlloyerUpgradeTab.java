package org.brahypno.sophisticated_smeltery.client;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeContainer;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiFuelModule;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiTankModule;

/** Keeps the existing 3x3 component and bucket-row layout, with eight ingredient tanks and one fuel slot. */
public final class AlloyerUpgradeTab extends UpgradeSettingsTab<AlloyerUpgradeContainer> {
    public AlloyerUpgradeTab(AlloyerUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, container.getUpgradeStack().getHoverName(), container.getUpgradeStack().getHoverName());
        addHideableChild(new TankDisplay(new Position(x + 5, y + 24), 0, 106, 40));
        for (int slot = 0; slot < 8; slot++)
            addHideableChild(new TankDisplay(new Position(x + 5 + slot % 4 * 27, y + 72 + slot / 4 * 31), slot + 1, 23, 26));
        addHideableChild(new FuelDisplay(new Position(x + 115, y + 41)));
        for (int slot = 0; slot < 9; slot++)
            addHideableChild(new SlotHint(new Position(x + 5 + slot % 3 * 18, y + 139 + slot / 3 * 18), slot));
        addHideableChild(new BucketTransferModeButton(new Position(x + 65 + BucketTransferModeButton.MODE_BUTTON_OFFSET, y + 138),
                () -> container.getUpgradeWrapper().getTransferDirection(), () -> container.action("bucketMode")));
        addHideableChild(new TankAction(new Position(x + 65, y + 163),
                () -> new ItemStack(container.getUpgradeWrapper().getContainerTarget() == 0
                        ? TinkerSmeltery.scorchedAlloyer.get() : TinkerSmeltery.scorchedTank.get(TankType.INGOT_TANK)),
                button -> container.action("bucketTarget"),
                () -> List.of(text(container.getUpgradeWrapper().getContainerTarget() == 0 ? "alloyer_output" : "alloyer_input",
                        container.getUpgradeWrapper().getContainerTarget()), text("alloyer_target_hint")),
                () -> container.getUpgradeWrapper().getContainerTarget()));
        for (int index = 0; index < Math.min(2, container.getUpgradeWrapper().getBackpackTanks().size()); index++) {
            int tank = index;
            addHideableChild(new TankAction(new Position(x + 5 + index * 24, y + 203),
                    () -> new ItemStack(ModItems.TANK_UPGRADE.get()), button -> container.transfer(tank, button == 0),
                    () -> List.of(text("backpack_tank", tank + 1), text("transfer_hint")), () -> tank + 1));
        }
        openTabDimension = new Dimension(138, container.getUpgradeWrapper().getBackpackTanks().isEmpty() ? 205 : 226);
    }
    private static Component text(String key, Object... args) { return MeltingUpgradeTab.text(key, args); }
    @Override protected void moveSlotsToTab() {
        var slots = getContainer().getSlots();
        for (int slot = 0; slot < 9; slot++) {
            slots.get(slot).x = x - screen.getGuiLeft() + 5 + slot % 3 * 18;
            slots.get(slot).y = y - screen.getGuiTop() + 139 + slot / 3 * 18;
        }
        slots.get(9).x = x - screen.getGuiLeft() + 65;
        slots.get(9).y = y - screen.getGuiTop() + 139;
        slots.get(10).x = x - screen.getGuiLeft() + 65 + BucketTransferModeButton.OUTPUT_SLOT_OFFSET;
        slots.get(10).y = y - screen.getGuiTop() + 139;
    }
    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        if (getWidth() <= 125) super.renderBg(graphics, minecraft, mouseX, mouseY);
        else {
            int half = getHeight() / 2;
            GuiHelper.renderTiledControlBackground(graphics, x, y, getWidth(), getHeight(), 134, 0, 122, 256, 0, 6, half, getHeight() - half);
            graphics.blit(GuiHelper.GUI_CONTROLS, x - 3, y, 128, 256 - getHeight(), 3, getHeight());
        }
        if (!getContainer().isOpen()) return;
        GuiHelper.renderSlotsBackground(graphics, x + 4, y + 138, 3, 3);
        BucketTransferModeButton.renderContainerRow(graphics, x + 65, y + 139, getContainer().getUpgradeWrapper().getContainers().getStackInSlot(0).isEmpty());
    }
    private final class TankDisplay extends WidgetBase {
        private final int target;
        private TankDisplay(Position position, int target, int width, int height) { super(position, new Dimension(width, height)); this.target = target; }
        private GuiTankModule module() {
            var wrapper = getContainer().getUpgradeWrapper();
            return new GuiTankModule(screen, target == 0 ? wrapper.getTank() : wrapper.getInputTank(target - 1),
                    x - screen.getGuiLeft(), y - screen.getGuiTop(), getWidth(), getHeight(), null);
        }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            graphics.fill(x - 1, y - 1, x + getWidth() + 1, y + getHeight() + 1, 0xff737373);
            graphics.fill(x, y, x + getWidth(), y + getHeight(), 0xff252525);
            module().draw(graphics);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (target > 0) graphics.drawString(font, Integer.toString(target), x + 2, y + 2, 0xffffffff, true);
        }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY) || button > 1) return false;
            getContainer().fluidClick(target, button == 1);
            return true;
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY)) module().renderTooltip(graphics, mouseX, mouseY);
        }
    }
    private final class SlotHint extends WidgetBase {
        private final int slot;
        private SlotHint(Position position, int slot) { super(position, new Dimension(16, 16)); this.slot = slot; }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) { }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (getContainer().getUpgradeWrapper().getComponents().getStackInSlot(slot).isEmpty()) {
                if (slot == 8) {
                    graphics.setColor(0.65F, 0.65F, 0.65F, 0.6F);
                    graphics.blit(HeatingStructureScreen.BACKGROUND, x + 1, y + 1, 176, 136, 14, 14);
                    graphics.setColor(1, 1, 1, 1);
                } else graphics.drawString(font, Integer.toString(slot + 1), x + 5, y + 4, 0xff707070, false);
            }
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY) && getContainer().getUpgradeWrapper().getComponents().getStackInSlot(slot).isEmpty())
                graphics.renderTooltip(font, font.split(text(slot == 8 ? "alloyer_fuel_hint" : "alloyer_input_hint", slot + 1), 180), mouseX, mouseY);
        }
    }
    private final class FuelDisplay extends WidgetBase {
        private GuiFuelModule gui;
        private Object displayedFuel;
        private int guiX, guiY;
        private FuelDisplay(Position position) { super(position, new Dimension(16, 88)); }
        private GuiFuelModule module() {
            var fuel = getContainer().getUpgradeWrapper().alloyer().fuel();
            if (gui == null || displayedFuel != fuel || guiX != x || guiY != y) {
                displayedFuel = fuel; guiX = x; guiY = y;
                gui = new GuiFuelModule(screen, fuel, x - screen.getGuiLeft(), y - screen.getGuiTop(),
                        16, 88, x - screen.getGuiLeft() + 1, y - screen.getGuiTop() - 16, false, HeatingStructureScreen.BACKGROUND);
            }
            return gui;
        }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            graphics.fill(x - 1, y - 1, x + 17, y + 89, 0xff737373);
            graphics.fill(x, y, x + 16, y + 88, 0xff252525);
            module().draw(graphics);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY) || button > 1) return false;
            getContainer().fluidClick(9, button == 1);
            return true;
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY)) module().addTooltip(graphics, mouseX, mouseY, getContainer().getUpgradeWrapper().alloyer().fuel().hasSources());
        }
    }
    private final class TankAction extends WidgetBase {
        private final Supplier<ItemStack> icon;
        private final IntConsumer action;
        private final Supplier<List<Component>> tooltip;
        private final IntSupplier number;
        private TankAction(Position position, Supplier<ItemStack> icon, IntConsumer action, Supplier<List<Component>> tooltip, IntSupplier number) {
            super(position, new Dimension(18, 18)); this.icon = icon; this.action = action; this.tooltip = tooltip; this.number = number;
        }
        @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            graphics.blit(HeatingStructureScreen.BACKGROUND, x, y, 124, isHovered ? 69 : 45, 18, 18);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.renderItem(icon.get(), x + 1, y + 1);
            if (number.getAsInt() > 0) graphics.drawString(font, Integer.toString(number.getAsInt()), x + 11, y + 9, 0xffffffff, true);
        }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY) || button > 1) return false;
            action.accept(button); return true;
        }
        @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY)) graphics.renderTooltip(font, tooltip.get(), java.util.Optional.empty(), mouseX, mouseY);
        }
    }
}
