package org.brahypno.sophisticated_smeltery.client;

import com.mojang.math.Axis;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;

/** Native bucket mode artwork and tooltip, independent of the surrounding layout. */
public final class BucketTransferModeButton extends WidgetBase {
    public static final int OUTPUT_SLOT_OFFSET = 50;
    public static final int MODE_BUTTON_OFFSET = 19;
    private static final ElementScreen AUTO = new ElementScreen(HeatingStructureScreen.BACKGROUND, 176, 186, 16, 16, 256, 256);
    private static final ElementScreen EMPTY = AUTO.shift(16, 0);
    private static final ElementScreen FILL = EMPTY.shift(16, 0);
    private static final ElementScreen BUCKET = FILL.shift(16, 0);
    private static final ElementScreen HOVER = AUTO.move(176, 202, 18, 18);
    private static final ElementScreen ARROW = AUTO.move(124, 87, 18, 14);
    private static final ElementScreen ARROW_SHAFT = ARROW.move(124, 87, 18, 4);
    private final Supplier<TransferDirection> direction;
    private final Runnable cycle;

    public BucketTransferModeButton(Position position, Supplier<TransferDirection> direction, Runnable cycle) {
        super(position, new Dimension(18, 18));
        this.direction = direction;
        this.cycle = cycle;
    }

    public static void renderContainerRow(GuiGraphics graphics, int inputX, int inputY, boolean inputEmpty) {
        graphics.blit(HeatingStructureScreen.BACKGROUND, inputX - 1, inputY - 1, 124, 45, 18, 18);
        graphics.blit(HeatingStructureScreen.BACKGROUND, inputX + OUTPUT_SLOT_OFFSET - 1, inputY - 1, 124, 45, 18, 18);
        if (inputEmpty) BUCKET.draw(graphics, inputX, inputY);
        // Rotate the native downward arrow to fit the retained horizontal slots.
        graphics.pose().pushPose();
        graphics.pose().translate(inputX + 18, inputY + 17, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(-90));
        // Extend only the shaft behind the button; retain the full native arrowhead.
        graphics.pose().pushPose();
        graphics.pose().scale(1, 4, 1);
        ARROW_SHAFT.draw(graphics, 0, 0);
        graphics.pose().popPose();
        ARROW.draw(graphics, 0, 16);
        graphics.pose().popPose();
    }

    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        graphics.blit(HeatingStructureScreen.BACKGROUND, x, y, 124, 69, 18, 18);
        if (isHovered) HOVER.draw(graphics, x, y);
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        (switch (direction.get()) {
            case EMPTY_ITEM -> EMPTY;
            case FILL_ITEM -> FILL;
            default -> AUTO;
        }).draw(graphics, x + 1, y + 1);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY)) return false;
        cycle.run();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
        return true;
    }

    @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return;
        String mode = switch (direction.get()) {
            case EMPTY_ITEM -> "empty_item";
            case FILL_ITEM -> "fill_item";
            default -> "auto";
        };
        graphics.renderTooltip(font, List.of(
                TConstruct.makeTranslation("gui", "tank.bucket." + mode + ".title"),
                TConstruct.makeTranslation("gui", "tank.bucket." + mode + ".description").withStyle(ChatFormatting.GRAY),
                TConstruct.makeTranslation("gui", "tank.bucket.clickable").withStyle(ChatFormatting.GRAY)),
                Optional.empty(), mouseX, mouseY);
    }
}
