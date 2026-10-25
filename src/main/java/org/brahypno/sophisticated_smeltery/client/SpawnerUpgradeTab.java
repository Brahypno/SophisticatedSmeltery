package org.brahypno.sophisticated_smeltery.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.brahypno.sophisticated_smeltery.upgrade.SpawnerUpgradeContainer;

public final class SpawnerUpgradeTab extends UpgradeSettingsTab<SpawnerUpgradeContainer> {
    public SpawnerUpgradeTab(SpawnerUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, Component.translatable("gui.sophisticated_smeltery.spawner"), container.getUpgradeStack().getHoverName());
        openTabDimension = new Dimension(Math.max(32,
                26 + font.width(Component.translatable("gui.sophisticated_smeltery.spawner"))), 54);
        addHideableChild(new WidgetBase(new Position(x + 7, y + 28), Dimension.SQUARE_18) {
            @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) { }
            @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }
            @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
                if (isMouseOver(mouseX, mouseY) && getContainer().getUpgradeWrapper().inventory().getStackInSlot(0).isEmpty())
                    graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.sophisticated_smeltery.spawner_slot")), mouseX, mouseY);
            }
        });
    }
    @Override protected void moveSlotsToTab() {
        var slot = getContainer().getSlots().get(0);
        slot.x = x - screen.getGuiLeft() + 8;
        slot.y = y - screen.getGuiTop() + 29;
    }
    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        super.renderBg(graphics, minecraft, mouseX, mouseY);
        if (getContainer().isOpen()) GuiHelper.renderSlotsBackground(graphics, x + 7, y + 28, 1, 1);
    }
}
