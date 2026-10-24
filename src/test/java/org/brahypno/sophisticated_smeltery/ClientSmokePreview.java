package org.brahypno.sophisticated_smeltery;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import org.brahypno.sophisticated_smeltery.client.BucketTransferModeButton;
import org.brahypno.sophisticated_smeltery.client.MeltingHeatBar;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiMeltingModule;
import slimeknights.tconstruct.library.client.GuiUtil;

/** Opt-in client smoke test; never included in the main mod source set. */
@Mod.EventBusSubscriber(modid = "sophisticatedsmeltery", value = Dist.CLIENT)
public final class ClientSmokePreview {
    private static boolean opened;
    private static boolean saved;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.clientSmoke") || opened || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null && minecraft.getOverlay() == null) {
            long window = minecraft.getWindow().getWindow();
            org.lwjgl.glfw.GLFW.glfwRestoreWindow(window);
            org.lwjgl.glfw.GLFW.glfwShowWindow(window);
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(window, 854, 480);
            org.lwjgl.glfw.GLFW.glfwFocusWindow(window);
            opened = true;
            minecraft.setScreen(new Preview());
        }
    }

    @SubscribeEvent public static void screenshot(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof Preview preview) || preview.frames < 10 || saved) return;
        var target = Minecraft.getInstance().getMainRenderTarget();
        if (target.width < 128 || target.height < 128) {
            if (preview.frames > 200) throw new IllegalStateException("Client framebuffer stayed minimized; screenshot verification did not run");
            return;
        }
        saved = true;
        event.getGuiGraphics().flush();
        Path destination = Boolean.getBoolean("sophisticatedsmeltery.wideSlotsSmoke") ? Path.of("../wide-slots-smoke.png")
                : Boolean.getBoolean("sophisticatedsmeltery.heatBarSmoke") ? Path.of("../heat-bars-smoke.png")
                : Boolean.getBoolean("sophisticatedsmeltery.bucketSmoke") ? Path.of("../bucket-controls-smoke.png")
                : Path.of("../build/client-smoke.png");
        try (var image = Screenshot.takeScreenshot(target)) {
            Files.createDirectories(destination.toAbsolutePath().getParent());
            image.writeToFile(destination);
            SophisticatedSmeltery.LOGGER.info("Client smoke test saved to {}", destination.toAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot save client smoke screenshot", e);
        }
        Minecraft.getInstance().stop();
    }

    private static final class Preview extends Screen {
        private int frames;
        private BucketTransferModeButton[] bucketModes;
        private Preview() { super(Component.literal("Melting Upgrade Preview")); }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            frames++;
            graphics.fill(0, 0, width, height, 0xff19212d);
            if (Boolean.getBoolean("sophisticatedsmeltery.wideSlotsSmoke")) {
                renderWideSlots(graphics);
                return;
            }
            if (Boolean.getBoolean("sophisticatedsmeltery.heatBarSmoke")) {
                renderHeatBars(graphics);
                return;
            }
            if (Boolean.getBoolean("sophisticatedsmeltery.bucketSmoke")) {
                renderBucketModes(graphics, partialTicks);
                return;
            }
            graphics.drawCenteredString(font, "Sophisticated Smeltery - controller icons", width / 2, height / 2 - 70, 0xfff1f5fa);
            ItemStack blank = new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get());
            ItemStack[] stacks = {blank, ControllerBinding.bind(blank, new ItemStack(TinkerSmeltery.smelteryController)),
                    ControllerBinding.bind(blank, new ItemStack(TinkerSmeltery.foundryController))};
            for (int index = 0; index < stacks.length; index++) {
                int center = width / 2 + (index - 1) * 110;
                graphics.pose().pushPose();
                graphics.pose().translate(center - 32, height / 2 - 32, 0);
                graphics.pose().scale(4, 4, 1);
                graphics.renderItem(stacks[index], 0, 0);
                graphics.pose().popPose();
                graphics.drawCenteredString(font, stacks[index].getHoverName(), center, height / 2 + 44, 0xfff1f5fa);
            }
        }

        private void renderBucketModes(GuiGraphics graphics, float partialTicks) {
            graphics.drawCenteredString(font, "Native bucket transfer modes", width / 2, height / 2 - 70, 0xfff1f5fa);
            TransferDirection[] modes = {TransferDirection.AUTO, TransferDirection.EMPTY_ITEM, TransferDirection.FILL_ITEM};
            if (bucketModes == null) {
                bucketModes = new BucketTransferModeButton[3];
                for (int index = 0; index < 3; index++) {
                    TransferDirection mode = modes[index];
                    bucketModes[index] = new BucketTransferModeButton(new Position(width / 2 + (index - 1) * 110 - 10, height / 2 - 1), () -> mode, () -> {});
                }
            }
            int hoverX = bucketModes[2].getX() + 9;
            int hoverY = bucketModes[2].getY() + 9;
            for (int index = 0; index < 3; index++) {
                int center = width / 2 + (index - 1) * 110;
                graphics.fill(center - 40, height / 2 - 32, center + 40, height / 2 + 24, 0xffc6c6c6);
                BucketTransferModeButton.renderContainerRow(graphics, center - 27, height / 2 - 25, true);
                bucketModes[index].render(graphics, hoverX, hoverY, partialTicks);
            }
            bucketModes[2].renderTooltip(this, graphics, hoverX, hoverY);
        }

        private void renderHeatBars(GuiGraphics graphics) {
            graphics.drawCenteredString(font, "Fixed heat-bar width across all states", width / 2, height / 2 - 65, 0xfff1f5fa);
            MeltingHeatBar.State[] states = {null, MeltingHeatBar.State.HEATING, MeltingHeatBar.State.HEATING,
                    MeltingHeatBar.State.NO_HEAT, MeltingHeatBar.State.NO_SPACE, MeltingHeatBar.State.UNMELTABLE};
            String[] labels = {"Empty", "0%", "50%", "No heat", "Full", "No recipe"};
            for (int index = 0; index < states.length; index++) {
                int center = width / 2 + (index * 2 - 5) * 31;
                graphics.pose().pushPose();
                graphics.pose().translate(center - 27, height / 2 - 27, 0);
                graphics.pose().scale(3, 3, 1);
                GuiHelper.renderSlotsBackground(graphics, 0, 0, 1, 1);
                if (states[index] != null) graphics.renderItem(new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT), 1, 1);
                MeltingHeatBar.render(graphics, 1 - MeltingHeatBar.WIDTH, 1, states[index], index == 2 ? 0.5f : 0);
                graphics.pose().popPose();
                graphics.drawCenteredString(font, labels[index], center, height / 2 + 38, 0xfff1f5fa);
            }
        }

        private void renderWideSlots(GuiGraphics graphics) {
            graphics.drawCenteredString(font, "18x18 slots / native 22x18 slots - same 16x16 items", width / 2, 15, 0xfff1f5fa);
            var bars = GuiMeltingModule.makeProgressBars(HeatingStructureScreen.BACKGROUND);
            MeltingHeatBar.State[] states = {null, MeltingHeatBar.State.HEATING, MeltingHeatBar.State.HEATING,
                    MeltingHeatBar.State.NO_HEAT, MeltingHeatBar.State.NO_SPACE, MeltingHeatBar.State.UNMELTABLE};
            net.minecraft.world.item.Item[] items = {net.minecraft.world.item.Items.COPPER_INGOT, net.minecraft.world.item.Items.IRON_INGOT,
                    net.minecraft.world.item.Items.GOLD_INGOT, net.minecraft.world.item.Items.RAW_IRON};
            for (int layout = 0; layout < 2; layout++) {
                int pitch = layout == 0 ? 18 : 22;
                int left = (width - 12 * pitch) / 2;
                int top = layout == 0 ? 48 : 119;
                graphics.drawCenteredString(font, layout == 0 ? "Current: 18 pixels per column" : "Option 2: 22 pixels per column", width / 2, top - 15, 0xfff1f5fa);
                for (int index = 0; index < 24; index++) {
                    int x = left + index % 12 * pitch;
                    int y = top + index / 12 * 18;
                    var state = states[index % states.length];
                    float progress = index % states.length == 2 ? 0.5f : 0;
                    if (layout == 0) {
                        GuiHelper.renderSlotsBackground(graphics, x, y, 1, 1);
                        if (state != null) graphics.renderItem(new ItemStack(items[index % items.length]), x + 1, y + 1);
                        MeltingHeatBar.render(graphics, x + 1 - MeltingHeatBar.WIDTH, y + 1, state, progress);
                    } else {
                        graphics.blit(HeatingStructureScreen.BACKGROUND, x, y, 0, 238, 22, 18);
                        if (state != null) {
                            graphics.renderItem(new ItemStack(items[index % items.length]), x + 5, y + 1);
                            var bar = switch (state) {
                                case HEATING -> bars.progress();
                                case NO_HEAT -> bars.noHeat();
                                case NO_SPACE -> bars.noSpace();
                                case UNMELTABLE -> bars.unmeltable();
                            };
                            GuiUtil.drawProgressUp(graphics, bar, x + 1, y + 1, state == MeltingHeatBar.State.HEATING ? progress : 1);
                        }
                    }
                }
            }
            ItemStack blank = new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get());
            String key = "item.sophisticatedsmeltery.melting_upgrade.tooltip";
            String translated = net.minecraft.client.resources.language.I18n.get(key);
            if (!net.minecraft.client.resources.language.I18n.exists(key)
                    || blank.getTooltipLines(null, net.minecraft.world.item.TooltipFlag.NORMAL).stream().noneMatch(line -> line.getString().equals(translated))) {
                throw new IllegalStateException("Blank melting upgrade tooltip did not resolve through the actual item tooltip");
            }
            graphics.renderItem(blank, 30, 187);
            graphics.renderTooltip(font, blank, 50, 194);
        }
    }
}
