package org.brahypno.sophisticated_smeltery.client;

import com.mojang.blaze3d.platform.Lighting;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;

/** Controller items for the mode; the native powered lever seen straight from the front. */
public final class TransmuteControlButton extends WidgetBase {
    private final MeltingUpgradeContainer container;
    private final boolean alloy;

    public TransmuteControlButton(Position position, MeltingUpgradeContainer container, boolean alloy) {
        super(position, new Dimension(18, 18));
        this.container = container;
        this.alloy = alloy;
    }

    public boolean isAlloyButton() { return alloy; }

    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        graphics.blit(HeatingStructureScreen.BACKGROUND, x, y, 124, 69, 18, 18);
        if (isHovered) graphics.blit(HeatingStructureScreen.BACKGROUND, x, y, 176, 202, 18, 18);
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var wrapper = container.getUpgradeWrapper();
        if (!alloy) {
            ItemStack icon = switch (wrapper.getTransmuteMode()) {
                case 1 -> new ItemStack(TinkerSmeltery.smelteryController);
                case 2 -> new ItemStack(TinkerSmeltery.foundryController);
                default -> new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("esotericism_tinker", "transmute_controller")));
            };
            graphics.renderItem(icon, x + 1, y + 1);
            return;
        }
        var state = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("esotericism_tinker", "ashen_alloy_switch"))
                .defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.POWERED, wrapper.isTransmuteAlloying());
        graphics.flush();
        Lighting.setupForFlatItems();
        graphics.pose().pushPose();
        graphics.pose().translate(x + 1, y + 17, 150);
        graphics.pose().scale(16, -16, 16);
        minecraft.getBlockRenderer().renderSingleBlock(state, graphics.pose(), graphics.bufferSource(), 0xf000f0, OverlayTexture.NO_OVERLAY);
        graphics.flush();
        graphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY)) return false;
        container.action(alloy ? "transmuteAlloying" : "transmuteMode");
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
        return true;
    }

    @Override public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return;
        var wrapper = container.getUpgradeWrapper();
        String key = alloy ? (wrapper.isTransmuteAlloying() ? "transmute_alloy_on" : "transmute_alloy_off")
                : "transmute_mode_" + wrapper.getTransmuteMode();
        graphics.renderTooltip(font, List.of(MeltingUpgradeTab.text(key),
                MeltingUpgradeTab.text(alloy ? "transmute_alloy_hint" : "transmute_mode_hint")), Optional.empty(), mouseX, mouseY);
    }
}
