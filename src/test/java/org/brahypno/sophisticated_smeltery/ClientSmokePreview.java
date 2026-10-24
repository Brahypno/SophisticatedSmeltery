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
        Path destination = Path.of("../build/client-smoke.png");
        try (var image = Screenshot.takeScreenshot(target)) {
            Files.createDirectories(destination.toAbsolutePath().getParent());
            image.writeToFile(destination);
            SophisticatedSmeltery.LOGGER.info("Client icon smoke test saved to {}", destination.toAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot save client smoke screenshot", e);
        }
        Minecraft.getInstance().stop();
    }

    private static final class Preview extends Screen {
        private int frames;
        private Preview() { super(Component.literal("Melting Upgrade Preview")); }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            frames++;
            graphics.fill(0, 0, width, height, 0xff19212d);
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
    }
}
