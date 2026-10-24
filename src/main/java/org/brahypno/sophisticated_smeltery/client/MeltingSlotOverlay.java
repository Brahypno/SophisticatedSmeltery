package org.brahypno.sophisticated_smeltery.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryControlBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiMeltingModule;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;

final class MeltingSlotOverlay extends UpgradeInventoryControlBase {
    private final StorageScreenBase<?> screen;
    private final MeltingUpgradeContainer container;
    private final ToggleButton<Boolean> heatingSwitch;
    private final GuiMeltingModule.ProgressBars bars = GuiMeltingModule.makeProgressBars(HeatingStructureScreen.BACKGROUND);
    MeltingSlotOverlay(StorageScreenBase<?> screen, MeltingUpgradeContainer container) {
        this.screen = screen;
        this.container = container;
        int upgradeSlot = screen.getMenu().getUpgradeContainers().entrySet().stream()
                .filter(entry -> entry.getValue() == container).mapToInt(java.util.Map.Entry::getKey).findFirst().orElse(0);
        heatingSwitch = new ToggleButton<>(new Position(-22, 8 + upgradeSlot * 16), ButtonDefinitions.UPGRADE_SWITCH,
                button -> container.action("heating"), () -> container.getUpgradeWrapper().isHeating());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        var wrapper = container.getUpgradeWrapper();
        heatingSwitch.render(graphics, mouseX - screen.getGuiLeft(), mouseY - screen.getGuiTop(), 0);
        int count = Math.min(wrapper.getMeltingSlotCount(), screen.getMenu().getNumberOfStorageInventorySlots());
        for (int index = 0; index < count; index++) {
            Slot slot = screen.getMenu().getSlot(index);
            if (!slot.isActive()) continue;
            int color = wrapper.isHeating() ? 0xffff9a35 : 0xff909090;
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 1, color);
            var progress = wrapper.getFurnace().progressAt(index);
            int required = progress.getInt("required");
            int time = progress.getInt("time");
            if (slot.hasItem()) {
                var bar = bars.progress();
                float fraction = 1;
                if (required == 0) bar = bars.unmeltable();
                else if (progress.getInt("temperature") > currentTemperature()) bar = bars.noHeat();
                else if (time < 0) bar = bars.noSpace();
                else fraction = Math.min(1, (float)time / required);
                GuiUtil.drawProgressUp(graphics, bar, slot.x + 13, slot.y, fraction);
            }
        }
    }

    private int currentTemperature() {
        var fuel = container.getUpgradeWrapper().getFurnace().fuel();
        return fuel.hasFuel() ? fuel.getTemperature() : 0;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return heatingSwitch.mouseClicked(mouseX - screen.getGuiLeft(), mouseY - screen.getGuiTop(), button);
    }

    @Override public void renderTooltip(StorageScreenBase<?> screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (heatingSwitch.isMouseOver(mouseX - screen.getGuiLeft(), mouseY - screen.getGuiTop())) {
            graphics.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, MeltingUpgradeTab.text(container.getUpgradeWrapper().isHeating() ? "heating_on" : "heating_off"), mouseX, mouseY);
            return;
        }
        int count = Math.min(container.getUpgradeWrapper().getMeltingSlotCount(), screen.getMenu().getNumberOfStorageInventorySlots());
        for (int index = 0; index < count; index++) {
            Slot slot = screen.getMenu().getSlot(index);
            if (!slot.isActive() || !slot.hasItem() || !GuiUtil.isHovered(mouseX - screen.getGuiLeft(), mouseY - screen.getGuiTop(), slot.x + 13, slot.y, 3, 16)) continue;
            var progress = container.getUpgradeWrapper().getFurnace().progressAt(index);
            String state = progress.getInt("required") == 0 ? "no_recipe"
                    : progress.getInt("temperature") > currentTemperature() ? "no_heat"
                    : progress.getInt("time") < 0 ? "no_space" : null;
            if (state != null) graphics.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, Component.translatable("gui.tconstruct.melting." + state), mouseX, mouseY);
            return;
        }
    }
}
