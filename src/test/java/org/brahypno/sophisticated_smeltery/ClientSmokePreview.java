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
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class ClientSmokePreview {
    private static boolean opened;
    private static boolean saved;
    private static boolean creatingControllerWorld;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.clientSmoke") || opened || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean controllerPreview = Boolean.getBoolean("sophisticatedsmeltery.backpackControllerSmoke");
        if (controllerPreview && minecraft.level == null) {
            if (!creatingControllerWorld && minecraft.getOverlay() == null && (minecraft.screen instanceof net.minecraft.client.gui.screens.TitleScreen
                    || minecraft.screen != null && minecraft.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen"))) {
                creatingControllerWorld = true;
                minecraft.createWorldOpenFlows().createFreshLevel("controller-test-" + System.currentTimeMillis(),
                        new net.minecraft.world.level.LevelSettings("Controller model test", net.minecraft.world.level.GameType.CREATIVE,
                                false, net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(),
                                net.minecraft.world.level.WorldDataConfiguration.DEFAULT), new net.minecraft.world.level.levelgen.WorldOptions(42L, false, false),
                        access -> access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                                .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
            }
            return;
        }
        if ((minecraft.screen != null || controllerPreview && minecraft.player != null) && minecraft.getOverlay() == null) {
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
                : Boolean.getBoolean("sophisticatedsmeltery.backpackControllerSmoke") ? Path.of("../backpack-controller-smoke.png")
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
        private ItemStack[] controllerBackpacks;
        private Preview() { super(Component.literal("Melting Upgrade Preview")); }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            frames++;
            graphics.fill(0, 0, width, height, 0xff19212d);
            if (Boolean.getBoolean("sophisticatedsmeltery.backpackControllerSmoke")) {
                renderControllerBackpacks(graphics);
                return;
            }
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

        private void renderControllerBackpacks(GuiGraphics graphics) {
            if (controllerBackpacks == null) {
                controllerBackpacks = new ItemStack[3];
                ItemStack[] upgrades = {ItemStack.EMPTY,
                        ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), new ItemStack(TinkerSmeltery.smelteryController)),
                        ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), new ItemStack(TinkerSmeltery.foundryController))};
                for (int index = 0; index < 3; index++) {
                    controllerBackpacks[index] = new ItemStack(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.NETHERITE_BACKPACK.get());
                    new net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper(controllerBackpacks[index]).getRenderInfo()
                            .setUpgradeItems(index == 0 ? java.util.List.of() : java.util.List.of(upgrades[index]));
                }
                verifyControllerModels();
            }
            graphics.drawCenteredString(font, "Backpack controller - original / smeltery / foundry", width / 2, 15, 0xfff1f5fa);
            for (int index = 0; index < 3; index++) {
                int center = width / 2 + (index - 1) * 125;
                graphics.pose().pushPose();
                graphics.pose().translate(center - 40, 45, 0);
                graphics.pose().scale(5, 5, 1);
                graphics.renderItem(controllerBackpacks[index], 0, 0);
                graphics.pose().popPose();
                graphics.drawCenteredString(font, new String[]{"Original", "Smeltery", "Foundry"}[index], center, 140, 0xfff1f5fa);
                // The actual worn item context uses the same native backpack model.
                graphics.pose().pushPose();
                graphics.pose().translate(center, 180, 100);
                graphics.pose().scale(72, -72, 72);
                graphics.pose().mulPose(com.mojang.math.Axis.XP.rotationDegrees(15));
                graphics.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(150));
                Minecraft.getInstance().getItemRenderer().renderStatic(controllerBackpacks[index],
                        net.p3pp3rf1y.sophisticatedbackpacks.client.ClientEventHandler.WORN, 15728880,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, graphics.pose(), graphics.bufferSource(), null, 0);
                graphics.pose().popPose();
            }
            graphics.flush();
        }

        private void verifyControllerModels() {
            var renderer = Minecraft.getInstance().getItemRenderer();
            int[] counts = new int[3];
            for (int index = 0; index < 3; index++) {
                var model = renderer.getModel(controllerBackpacks[index], null, null, 0);
                if (!(model instanceof net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackDynamicModel.BackpackBakedModel))
                    throw new IllegalStateException("Keep native backpack model type for display items");
                model.getRenderPasses(controllerBackpacks[index], true);
                counts[index] = model.getQuads(null, null, net.minecraft.util.RandomSource.create(0),
                        net.minecraftforge.client.model.data.ModelData.EMPTY, null).size();
            }
            if (counts[1] != counts[0] + 6 || counts[2] != counts[0] + 6)
                throw new IllegalStateException("Bound backpacks must add exactly six controller faces: " + java.util.Arrays.toString(counts));
            var model = renderer.getModel(controllerBackpacks[0], null, null, 0);
            model.getRenderPasses(controllerBackpacks[0], true);
            if (model.getQuads(null, null, net.minecraft.util.RandomSource.create(0), net.minecraftforge.client.model.data.ModelData.EMPTY, null).size() != counts[0])
                throw new IllegalStateException("Controller geometry leaked to the next plain backpack");
            var info = new net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper(controllerBackpacks[1]).getRenderInfo();
            var controller = org.brahypno.sophisticated_smeltery.client.BackpackControllerModel.controller(info);
            var level = Minecraft.getInstance().level;
            var pos = Minecraft.getInstance().player.blockPosition().offset(2, 0, 0);
            for (var facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                var state = net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks.NETHERITE_BACKPACK.get().defaultBlockState()
                        .setValue(net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock.FACING, facing);
                level.setBlock(pos, state, 3);
                var block = (net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity)level.getBlockEntity(pos);
                block.setBackpack(controllerBackpacks[1]);
                var blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
                var data = blockModel.getModelData(level, pos, state, net.minecraftforge.client.model.data.ModelData.EMPTY);
                if (data.get(org.brahypno.sophisticated_smeltery.client.BackpackControllerModel.CONTROLLER) != controller)
                    throw new IllegalStateException("Actual block model data did not receive controller");
                int plain = blockModel.getQuads(state, null, net.minecraft.util.RandomSource.create(0), net.minecraftforge.client.model.data.ModelData.EMPTY, null).size();
                int bound = blockModel.getQuads(state, null, net.minecraft.util.RandomSource.create(0), data, null).size();
                if (bound != plain + 6) throw new IllegalStateException("Placed controller missing for " + facing);
                if (org.brahypno.sophisticated_smeltery.client.BackpackControllerModel.quads(controller, facing).stream()
                        .noneMatch(quad -> quad.getDirection() == facing && quad.getSprite().equals(Minecraft.getInstance().getBlockRenderer()
                                .getBlockModel(controller).getParticleIcon()))) throw new IllegalStateException("Controller face rotated incorrectly for " + facing);
            }
            SophisticatedSmeltery.LOGGER.info("BACKPACK CONTROLLER MODELS PASSED: item, worn context, four block orientations, plain reset and native model identity");
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
            String key = "item.sophisticated_smeltery.melting_upgrade.tooltip";
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
