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
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;

/** Keep the wrapper scheduled for cooling while the user switches off new pours. */
@Mod.EventBusSubscriber(modid = "sophisticatedsmeltery", value = Dist.CLIENT)
public final class CastingUpgradeSwitch extends ToggleButton<Boolean> {
    private final StorageScreenBase<?> screen;
    private final int slot;

    private static CastingUpgradeContainer container(StorageScreenBase<?> screen, int slot) {
        return screen.getMenu().getUpgradeContainers().get(slot) instanceof CastingUpgradeContainer casting ? casting : null;
    }

    private CastingUpgradeSwitch(StorageScreenBase<?> screen, int slot) {
        super(new Position(screen.getGuiLeft() - 22, screen.getGuiTop() + 8 + slot * 16), ButtonDefinitions.UPGRADE_SWITCH,
                button -> { var casting = container(screen, slot); if (casting != null) casting.action("enabled"); },
                () -> { var casting = container(screen, slot); return casting != null && casting.getUpgradeWrapper().isCastingEnabled(); });
        this.screen = screen;
        this.slot = slot;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        setVisible(container(screen, slot) != null);
        setPosition(new Position(screen.getGuiLeft() - 22, screen.getGuiTop() + 8 + slot * 16));
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        var previous = screen.getMenu().getUpgradeContainers().get(slot - 1);
        backing(graphics, slot, slot > 0 && (screen.getMenu().canDisableUpgrade(slot - 1)
                || previous instanceof CastingUpgradeContainer || previous instanceof MeltingUpgradeContainer));
        if (screen.getMenu().canDisableUpgrade(slot + 1)) backing(graphics, slot + 1, true);
        super.renderBg(graphics, minecraft, mouseX, mouseY);
    }

    private void backing(GuiGraphics graphics, int slot, boolean continuation) {
        int offset = continuation ? 1 : 0;
        graphics.blit(GuiHelper.GUI_CONTROLS, screen.getGuiLeft() - 25,
                screen.getGuiTop() + 5 + slot * 16 + offset, 0, 204 + offset, 7, 18 - offset);
    }

    @Override protected List<Component> getTooltip(StateData state) {
        return List.of(MeltingUpgradeTab.text(getState.get() ? "cast_enabled" : "cast_disabled"));
    }

    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof StorageScreenBase<?> storage) {
            // Include currently empty slots so installing an upgrade into an open screen works too.
            for (int slot = 0; slot < storage.getMenu().getStorageWrapper().getUpgradeHandler().getSlots(); slot++)
                event.addListener(new CastingUpgradeSwitch(storage, slot));
        }
    }

    @SubscribeEvent public static void tooltip(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof StorageScreenBase<?> storage)) return;
        for (var child : storage.children()) {
            if (child instanceof CastingUpgradeSwitch control && container(storage, control.slot) != null)
                control.renderTooltip(storage, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }
}
