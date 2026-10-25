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
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;

/** Use the upgrade-card switch while keeping the wrapper scheduled to service its fluid containers. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class AlloyerUpgradeSwitch extends ToggleButton<Boolean> {
    private final StorageScreenBase<?> screen;
    private final int slot;

    private static AlloyerUpgradeContainer container(StorageScreenBase<?> screen, int slot) {
        return screen.getMenu().getUpgradeContainers().get(slot) instanceof AlloyerUpgradeContainer alloyer ? alloyer : null;
    }

    private AlloyerUpgradeSwitch(StorageScreenBase<?> screen, int slot) {
        super(new Position(screen.getGuiLeft() - 22, screen.getGuiTop() + 8 + slot * 16), ButtonDefinitions.UPGRADE_SWITCH,
                button -> { var alloyer = container(screen, slot); if (alloyer != null) alloyer.action("heating"); },
                () -> { var alloyer = container(screen, slot); return alloyer != null && alloyer.getUpgradeWrapper().isHeating(); });
        this.screen = screen;
        this.slot = slot;
        updateTarget();
    }

    private void updateTarget() {
        setVisible(container(screen, slot) != null);
        setPosition(new Position(screen.getGuiLeft() - 22, screen.getGuiTop() + 8 + slot * 16));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        updateTarget();
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        updateTarget();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        var previous = screen.getMenu().getUpgradeContainers().get(slot - 1);
        backing(graphics, slot, slot > 0 && (screen.getMenu().canDisableUpgrade(slot - 1)
                || previous instanceof MeltingUpgradeContainer || previous instanceof CastingUpgradeContainer || previous instanceof AlloyerUpgradeContainer));
        if (screen.getMenu().canDisableUpgrade(slot + 1)) backing(graphics, slot + 1, true);
        super.renderBg(graphics, minecraft, mouseX, mouseY);
    }

    private void backing(GuiGraphics graphics, int slot, boolean continuation) {
        int offset = continuation ? 1 : 0;
        graphics.blit(GuiHelper.GUI_CONTROLS, screen.getGuiLeft() - 25,
                screen.getGuiTop() + 5 + slot * 16 + offset, 0, 204 + offset, 7, 18 - offset);
    }

    @Override protected List<Component> getTooltip(StateData state) {
        return List.of(MeltingUpgradeTab.text(getState.get() ? "heating_on" : "heating_off"), MeltingUpgradeTab.text("alloyer_switch_hint"));
    }

    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof StorageScreenBase<?> storage) {
            for (int slot = 0; slot < storage.getMenu().getStorageWrapper().getUpgradeHandler().getSlots(); slot++)
                event.addListener(new AlloyerUpgradeSwitch(storage, slot));
        }
    }

    @SubscribeEvent public static void tooltip(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof StorageScreenBase<?> storage)) return;
        for (var child : storage.children()) {
            if (child instanceof AlloyerUpgradeSwitch control && container(storage, control.slot) != null)
                control.renderTooltip(storage, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }
}
