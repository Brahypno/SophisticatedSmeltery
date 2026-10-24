package org.brahypno.sophisticated_smeltery.client;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import slimeknights.mantle.client.screen.ScalableElementScreen;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiMeltingModule;

/** One fixed pixel footprint for empty, heating and error states. */
public final class MeltingHeatBar {
    public static final int WIDTH = 2;
    public static final int HEIGHT = 16;
    private static final GuiMeltingModule.ProgressBars BARS = GuiMeltingModule.makeProgressBars(HeatingStructureScreen.BACKGROUND);

    public enum State { HEATING, NO_HEAT, NO_SPACE, UNMELTABLE }

    public static void render(GuiGraphics graphics, int x, int y, @Nullable State state, float progress) {
        // Keep the whole track visible even when there is no colored progress.
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xff373737);
        if (state == null) return;
        ScalableElementScreen bar = switch (state) {
            case HEATING -> BARS.progress();
            case NO_HEAT -> BARS.noHeat();
            case NO_SPACE -> BARS.noSpace();
            case UNMELTABLE -> BARS.unmeltable();
        };
        int height = state == State.HEATING ? Math.max(0, Math.min(HEIGHT, (int)(progress * HEIGHT + 0.5f))) : HEIGHT;
        if (height == 0) return;
        int offset = HEIGHT - height;
        // Native gradients have identical columns. Copy two columns at 1:1 scale,
        // rather than rasterizing a 3-to-2 fractional horizontal transformation.
        graphics.blit(bar.texture, x, y + offset, bar.x, bar.y + offset, WIDTH, height, bar.texW, bar.texH);
    }
}
