package org.brahypno.sophisticated_smeltery.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryControlBase;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;

final class MeltingSlotOverlay extends UpgradeInventoryControlBase {
    private static final int BAR_WIDTH = MeltingHeatBar.WIDTH;
    private static final int BAR_OFFSET = -BAR_WIDTH;
    private final StorageScreenBase<?> screen;
    private final MeltingUpgradeContainer container;
    MeltingSlotOverlay(StorageScreenBase<?> screen, MeltingUpgradeContainer container) {
        this.screen = screen;
        this.container = container;
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        var wrapper = container.getUpgradeWrapper();
        int count = Math.min(wrapper.getMeltingSlotCount(), screen.getMenu().getNumberOfStorageInventorySlots());
        for (int index = 0; index < count; index++) {
            Slot slot = screen.getMenu().getSlot(index);
            if (!slot.isActive()) continue;
            var progress = wrapper.getFurnace().progressAt(index);
            int required = progress.getInt("required");
            int time = progress.getInt("time");
            MeltingHeatBar.State state = null;
            float fraction = 1;
            if (slot.hasItem()) {
                state = MeltingHeatBar.State.HEATING;
                if (required == 0) state = MeltingHeatBar.State.UNMELTABLE;
                else if (progress.getInt("temperature") > currentTemperature()) state = MeltingHeatBar.State.NO_HEAT;
                else if (time < 0) state = MeltingHeatBar.State.NO_SPACE;
                else fraction = Math.min(1, (float)time / required);
            }
            MeltingHeatBar.render(graphics, slot.x + BAR_OFFSET, slot.y, state, fraction);
        }
    }

    private int currentTemperature() {
        var fuel = container.getUpgradeWrapper().getFurnace().fuel();
        return fuel.hasFuel() ? fuel.getTemperature() : 0;
    }

    private static Slot hoveredHeatBar(StorageScreenBase<?> screen, MeltingUpgradeContainer container, double mouseX, double mouseY) {
        int count = Math.min(container.getUpgradeWrapper().getMeltingSlotCount(), screen.getMenu().getNumberOfStorageInventorySlots());
        double x = mouseX - screen.getGuiLeft();
        double y = mouseY - screen.getGuiTop();
        for (int index = 0; index < count; index++) {
            Slot slot = screen.getMenu().getSlot(index);
            if (slot.isActive() && slot.hasItem() && x >= slot.x + BAR_OFFSET && x < slot.x + BAR_OFFSET + BAR_WIDTH
                    && y >= slot.y && y < slot.y + 16) return slot;
        }
        return null;
    }

    static boolean isHeatBarHovered(StorageScreenBase<?> screen, MeltingUpgradeContainer container, double mouseX, double mouseY) {
        return hoveredHeatBar(screen, container, mouseX, mouseY) != null;
    }

    @Override public void renderTooltip(StorageScreenBase<?> screen, GuiGraphics graphics, int mouseX, int mouseY) {
        Slot slot = hoveredHeatBar(screen, container, mouseX, mouseY);
        if (slot != null) {
            var progress = container.getUpgradeWrapper().getFurnace().progressAt(slot.getSlotIndex());
            String state = progress.getInt("required") == 0 ? "no_recipe"
                    : progress.getInt("temperature") > currentTemperature() ? "no_heat"
                    : progress.getInt("time") < 0 ? "no_space" : null;
            if (state != null) graphics.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, Component.translatable("gui.tconstruct.melting." + state), mouseX, mouseY);
        }
    }
}
