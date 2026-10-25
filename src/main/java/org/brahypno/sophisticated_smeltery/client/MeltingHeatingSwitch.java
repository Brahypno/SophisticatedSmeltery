package org.brahypno.sophisticated_smeltery.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeContainer;

/** Screen-level control: inventory controls are clipped by the storage scroll panel. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class MeltingHeatingSwitch extends ToggleButton<Boolean> {
    private final StorageScreenBase<?> screen;
    private final Target target;

    private static final class Target {
        private MeltingUpgradeContainer container;
        private int slot;
    }

    private MeltingHeatingSwitch(StorageScreenBase<?> screen) { this(screen, new Target()); }

    private MeltingHeatingSwitch(StorageScreenBase<?> screen, Target target) {
        super(new Position(0, 0), ButtonDefinitions.UPGRADE_SWITCH,
                button -> { if (target.container != null) target.container.action("heating"); },
                () -> target.container != null && target.container.getUpgradeWrapper().isHeating());
        this.screen = screen;
        this.target = target;
        updateTarget();
    }

    private void updateTarget() {
        target.container = null;
        for (var entry : screen.getMenu().getUpgradeContainers().entrySet()) {
            if (entry.getValue() instanceof MeltingUpgradeContainer container) {
                target.container = container;
                target.slot = entry.getKey();
                break;
            }
        }
        setVisible(target.container != null);
        setPosition(new Position(screen.getGuiLeft() - 22, screen.getGuiTop() + 8 + target.slot * 16));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        updateTarget();
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        var previous = screen.getMenu().getUpgradeContainers().get(target.slot - 1);
        renderSwitchBacking(graphics, target.slot,
                target.slot > 0 && (screen.getMenu().canDisableUpgrade(target.slot - 1)
                        || previous instanceof CastingUpgradeContainer || previous instanceof AlloyerUpgradeContainer));
        // The native following segment otherwise treats this always-enabled wrapper as a gap.
        if (screen.getMenu().canDisableUpgrade(target.slot + 1)) renderSwitchBacking(graphics, target.slot + 1, true);
        super.renderBg(graphics, minecraft, mouseX, mouseY);
    }

    private void renderSwitchBacking(GuiGraphics graphics, int slot, boolean continuation) {
        int offset = continuation ? 1 : 0;
        graphics.blit(GuiHelper.GUI_CONTROLS, screen.getGuiLeft() - 25,
                screen.getGuiTop() + 5 + slot * 16 + offset, 0, 204 + offset, 7, 18 - offset);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        updateTarget();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override protected List<Component> getTooltip(StateData state) {
        return List.of(MeltingUpgradeTab.text(getState.get() ? "heating_on" : "heating_off"));
    }

    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof StorageScreenBase<?> storage) event.addListener(new MeltingHeatingSwitch(storage));
    }

    @SubscribeEvent public static void tooltip(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof StorageScreenBase<?> storage)) return;
        for (var child : storage.children()) {
            if (child instanceof MeltingHeatingSwitch control)
                control.renderTooltip(storage, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }
}
