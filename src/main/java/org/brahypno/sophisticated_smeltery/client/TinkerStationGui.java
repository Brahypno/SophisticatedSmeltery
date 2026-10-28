package org.brahypno.sophisticated_smeltery.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.mantle.client.screen.ScalableElementScreen;
import slimeknights.tconstruct.library.client.Icons;
import slimeknights.tconstruct.tables.client.inventory.widget.BorderWidget;

/** The same texture regions used by TinkerStationScreen and InfoPanelScreen. */
final class TinkerStationGui {
    static final ResourceLocation TINKER = new ResourceLocation("tconstruct", "textures/gui/tinker.png");
    static final ResourceLocation PANEL = new ResourceLocation("tconstruct", "textures/gui/panel.png");
    private static final ScalableElementScreen BACKGROUND = scalable(4, 4, 118, 75);

    private TinkerStationGui() {}
    private static ElementScreen element(int u, int v, int width, int height) {
        return new ElementScreen(PANEL, u, v, width, height, 256, 256);
    }
    private static ScalableElementScreen scalable(int u, int v, int width, int height) {
        return new ScalableElementScreen(PANEL, u, v, width, height, 256, 256);
    }
    static BorderWidget border(boolean advanced) {
        int u = 126, v = advanced ? 83 : 0;
        var border = new BorderWidget();
        border.cornerTopLeft = element(u, v, 4, 4);
        border.cornerTopRight = element(u + 122, v, 4, 4);
        border.cornerBottomLeft = element(u, v + 79, 4, 4);
        border.cornerBottomRight = element(u + 122, v + 79, 4, 4);
        border.borderTop = scalable(u + 4, v, 118, 4);
        border.borderBottom = scalable(u + 4, v + 79, 118, 4);
        border.borderLeft = scalable(u, v + 4, 4, 75);
        border.borderRight = scalable(u + 122, v + 4, 4, 75);
        return border;
    }
    static void panel(GuiGraphics graphics, BorderWidget border, int x, int y, int width, int height) {
        border.setPosition(x, y);
        border.setSize(width, height);
        border.draw(graphics);
        BACKGROUND.drawScaled(graphics, x + 4, y + 4, width - 8, height - 8);
    }
    static void button(GuiGraphics graphics, int x, int y, int width, boolean advanced, boolean pressed, boolean hovered) {
        var button = (pressed ? Icons.BUTTON_PRESSED : hovered ? Icons.BUTTON_HOVERED : Icons.BUTTON)
                .shift(0, -18 * (advanced ? 1 : 2));
        button.draw(graphics, x, y);
    }
    static void scrollbar(GuiGraphics graphics, int x, int y, int height, int offset, int maxOffset, boolean advanced, boolean hovered) {
        int u = advanced ? 12 : 6;
        scalable(u, 88, 3, 8).drawScaledY(graphics, x, y + 4, height - 8);
        element(u + 3, 88, 3, 4).draw(graphics, x, y);
        element(u + 3, 92, 3, 4).draw(graphics, x, y + height - 4);
        element(u + (hovered ? 3 : 0), 83, 3, 5).draw(graphics, x, y + (height - 5) * offset / maxOffset);
    }
}
