package org.brahypno.sophisticated_smeltery;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.mantle.util.RetexturedHelper;
import net.minecraft.world.level.block.Blocks;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationAppearance;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
public final class StorageCompatibilityClientTest {
    private static boolean opened;
    private static boolean saved;
    private static boolean loadingAppearanceWorld;
    private static final List<String> ITEMS = List.of("melting_upgrade", "part_builder_upgrade", "tinker_station_upgrade", "tinkers_anvil_upgrade", "modifier_worktable_upgrade", "scorched_alloyer_upgrade", "spawner_upgrade", "advanced_spawner_upgrade",
            "advanced_transmute_upgrade", "seared_casting_upgrade", "advanced_seared_casting_upgrade", "scorched_casting_upgrade", "advanced_scorched_casting_upgrade");

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("sophisticatedsmeltery.storageCompatSmoke") || opened || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) mc.setScreen(new TitleScreen());
        boolean appearance = Boolean.getBoolean("sophisticatedsmeltery.workstationAppearanceSmoke");
        if (appearance && !loadingAppearanceWorld && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            loadingAppearanceWorld = true;
            // Native material IDs are resolved against server data when ItemStack.of reads the saved source.
            mc.createWorldOpenFlows().createFreshLevel("workstation-appearance-" + System.currentTimeMillis(),
                    new net.minecraft.world.level.LevelSettings("Workstation appearances", net.minecraft.world.level.GameType.CREATIVE,
                            false, net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(),
                            net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                    new net.minecraft.world.level.levelgen.WorldOptions(42L, false, false),
                    access -> access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                            .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (appearance ? loadingAppearanceWorld && mc.player != null && mc.screen == null
                : mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            opened = true;
            mc.options.guiScale().set(2);
            long window = mc.getWindow().getWindow();
            org.lwjgl.glfw.GLFW.glfwRestoreWindow(window);
            org.lwjgl.glfw.GLFW.glfwShowWindow(window);
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(window, 1280, 900);
            verifyCreativeTab();
            mc.setScreen(new Gallery());
        }
    }

    private static void verifyCreativeTab() {
        var tab = SophisticatedSmeltery.CREATIVE_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
        int families = 0;
        for (String prefix : List.of("", "storage_")) {
            boolean loaded = ModList.get().isLoaded(prefix.isEmpty() ? "sophisticatedbackpacks" : "sophisticatedstorage");
            if (loaded) families++;
            for (String name : ITEMS) {
                var id = SophisticatedSmeltery.id(prefix + name);
                if (BuiltInRegistries.ITEM.containsKey(id) != (loaded && (!name.equals("advanced_transmute_upgrade") || ModList.get().isLoaded("esotericism_tinker")))) throw new IllegalStateException("Wrong conditional registration: " + id);
            }
            if (loaded && !tab.contains(stack(prefix + "melting_upgrade"))) throw new IllegalStateException("Missing creative upgrade family: " + prefix);
        }
        String expected = ModList.get().isLoaded("sophisticatedbackpacks") ? "melting_upgrade" : "storage_melting_upgrade";
        if (families > 0 && !ItemStack.isSameItemSameTags(tab.getIconItem(), stack(expected)))
            throw new IllegalStateException("Tab icon must use this mod's " + expected);
        int expectedCount = families * (ModList.get().isLoaded("esotericism_tinker") ? 16 : 14);
        if (tab.getDisplayItems().size() != expectedCount || tab.getDisplayItems().stream().anyMatch(item ->
                !BuiltInRegistries.ITEM.getKey(item.getItem()).getNamespace().equals(SophisticatedSmeltery.MODID)))
            throw new IllegalStateException("Creative tab must contain only the loaded families of this mod's upgrades");
        SophisticatedSmeltery.LOGGER.info("HOST FAMILY TAB PASS: {} upgrades, icon {}", expectedCount, expected);
    }

    private static ItemStack stack(String path) {
        var id = SophisticatedSmeltery.id(path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalStateException("Missing upgrade: " + id);
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    @SubscribeEvent public static void rendered(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof Gallery gallery) || gallery.frames < 15 || saved) return;
        var mc = Minecraft.getInstance();
        if (mc.getMainRenderTarget().width < 128 || mc.getMainRenderTarget().height < 128) {
            if (gallery.frames > 200) throw new IllegalStateException("Client framebuffer stayed minimized");
            return;
        }
        saved = true;
        String variant = ModList.get().isLoaded("sophisticatedbackpacks")
                ? ModList.get().isLoaded("sophisticatedstorage") ? "both" : "backpacks-only" : "storage-only";
        event.getGuiGraphics().flush();
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(Path.of("../" + (Boolean.getBoolean("sophisticatedsmeltery.workstationAppearanceSmoke")
                    ? "workstation-appearances-" : "host-split-icons-") + variant + ".png"));
        } catch (Exception error) { throw new IllegalStateException(error); }
        SophisticatedSmeltery.LOGGER.info("HOST FAMILY CLIENT PASS: {} fixed host plates, frames, bound models and melting tab icon", variant);
        mc.stop();
    }

    private record Icon(ItemStack stack, String label, String host) {}
    private static List<Icon> workstationAppearances(String prefix, String host) {
        var icons = new ArrayList<Icon>();
        for (var entry : java.util.Map.of("Oak station", Blocks.OAK_PLANKS, "Crimson station", Blocks.CRIMSON_PLANKS).entrySet())
            icons.add(new Icon(TinkerStationAppearance.bind(stack(prefix + "tinker_station_upgrade"),
                    RetexturedHelper.setTexture(new ItemStack(TinkerTables.tinkerStation), entry.getValue())), entry.getKey(), host));
        for (boolean scorched : List.of(false, true)) {
            var block = scorched ? TinkerTables.scorchedAnvil.asItem() : TinkerTables.tinkersAnvil.asItem();
            for (String material : List.of("cobalt", "manyullyn"))
                icons.add(new Icon(TinkerStationAppearance.bind(stack(prefix + "tinkers_anvil_upgrade"),
                        ((IMaterialItem)block).withMaterialForDisplay(MaterialVariantId.parse("tconstruct:" + material))),
                        (scorched ? "Scorch " : "Anvil ") + material, host));
            icons.add(new Icon(TinkerStationAppearance.bind(stack(prefix + "tinkers_anvil_upgrade"),
                    RetexturedHelper.setTexture(new ItemStack(block), Blocks.DIAMOND_BLOCK)),
                    scorched ? "Scorch diamond" : "Anvil diamond", host));
        }
        var signatures = new java.util.HashSet<String>();
        for (var icon : icons) {
            var model = Minecraft.getInstance().getItemRenderer().getModel(icon.stack, null, null, 0);
            var quads = model.getQuads(null, null, RandomSource.create(0));
            var signature = quads.stream().map(quad -> quad.getSprite().contents().name()
                    + java.util.Arrays.toString(quad.getVertices())).toList().toString();
            if (!signatures.add(signature)) throw new IllegalStateException("Workstation appearance was lost: " + icon.label);
        }
        SophisticatedSmeltery.LOGGER.info("WORKSTATION APPEARANCE PASS: {} native wood/material/retexture models for {}", signatures.size(), host);
        return icons;
    }
    private static final class Gallery extends Screen {
        private int frames;
        private final List<List<Icon>> families = new ArrayList<>();
        private Gallery() {
            super(Component.literal("Host-specific upgrades"));
            for (String prefix : List.of("", "storage_")) {
                String host = prefix.isEmpty() ? "sophisticatedbackpacks" : "sophisticatedstorage";
                if (!ModList.get().isLoaded(host)) continue;
                if (Boolean.getBoolean("sophisticatedsmeltery.workstationAppearanceSmoke")) {
                    families.add(workstationAppearances(prefix, host));
                    continue;
                }
                List<Icon> icons = new ArrayList<>();
                icons.add(new Icon(new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(host, "upgrade_base"))), "Native plate", host));
                icons.add(new Icon(stack(prefix + "melting_upgrade"), "Melting", host));
                icons.add(new Icon(ControllerBinding.bind(stack(prefix + "melting_upgrade"), new ItemStack(TinkerSmeltery.smelteryController)), "Bound Smeltery", host));
                for (String name : ITEMS) {
                    if (name.equals("melting_upgrade") || name.equals("advanced_transmute_upgrade") && !ModList.get().isLoaded("esotericism_tinker")) continue;
                    icons.add(new Icon(stack(prefix + name), name.replace("_upgrade", "").replace("scorched_alloyer", "alloyer").replace("scorched_casting", "Scorch cast").replace("seared_casting", "Seared cast").replace("advanced_", "Adv "), host));
                }
                if (ModList.get().isLoaded("esotericism_tinker"))
                    icons.add(new Icon(ControllerBinding.bind(stack(prefix + "advanced_transmute_upgrade"),
                            new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("esotericism_tinker", "transmute_controller")))), "Bound Adv Transmute", host));
                families.add(icons);
            }
        }
        private void renderIcon(GuiGraphics graphics, Icon icon, int x, int y) {
            var model = Minecraft.getInstance().getItemRenderer().getModel(icon.stack, null, null, 0);
            var quads = model.getQuads(null, null, RandomSource.create(0));
            if (quads.isEmpty() || quads.stream().anyMatch(quad -> quad.getSprite().contents().name().getPath().equals("missingno")))
                throw new IllegalStateException("Missing icon geometry/texture: " + icon.stack);
            var plate = new ResourceLocation(icon.host, "item/upgrade_base");
            if (quads.stream().noneMatch(quad -> quad.getSprite().contents().name().equals(plate)))
                throw new IllegalStateException("Upgrade lost its fixed host plate: " + icon.stack);
            var name = BuiltInRegistries.ITEM.getKey(icon.stack.getItem()).getPath();
            if ((name.contains("advanced_") || name.endsWith("tinkers_anvil_upgrade")) && quads.stream().noneMatch(quad -> quad.getSprite().contents().name().equals(new ResourceLocation(icon.host, "item/advanced_magnet_upgrade"))))
                throw new IllegalStateException("Wrong advanced frame for " + icon.stack);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(2.5f, 2.5f, 1);
            graphics.renderItem(icon.stack, 0, 0);
            graphics.pose().popPose();
            graphics.drawString(font, icon.label, x, y + 43, 0xffdddddd);
        }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            frames++;
            graphics.fill(0, 0, width, height, 0xff202936);
            for (int family = 0; family < families.size(); family++) {
                var icons = families.get(family);
                int top = 12 + family * 178;
                graphics.drawString(font, icons.get(0).host, 15, top, 0xffffffff);
                for (int index = 0; index < icons.size(); index++) renderIcon(graphics, icons.get(index), 18 + index % 6 * 103, top + 18 + index / 6 * 72);
            }
            var icon = SophisticatedSmeltery.CREATIVE_TAB.get().getIconItem();
            if (!icon.isEmpty()) {
                String host = BuiltInRegistries.ITEM.getKey(icon.getItem()).getPath().startsWith("storage_") ? "sophisticatedstorage" : "sophisticatedbackpacks";
                renderIcon(graphics, new Icon(icon, "Actual creative tab icon", host), 18, 18 + families.size() * 178);
            }
        }
    }
}
