package org.brahypno.sophisticated_smeltery;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationRecipeTransfer;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeContainer;
import org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeWrapper;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;
import slimeknights.tconstruct.tools.TinkerToolParts;

public final class TinkerJeiTransferClientTest {
    private static IJeiRuntime runtime;
    private static int stage, ticks;
    private static int fixtureTick;
    private static ItemStack tool;

    @JeiPlugin
    public static class TestPlugin implements IModPlugin {
        @Override public ResourceLocation getPluginUid() { return SophisticatedSmeltery.id("jei_transfer_test"); }
        @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
    }

    @Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID, value = Dist.CLIENT)
    public static class Events {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (Boolean.getBoolean("sophisticatedsmeltery.tinkerJeiSmoke") && event.phase == TickEvent.Phase.END) run();
        }
    }

    private static void run() {
        if (stage == 8) return;
        var mc = Minecraft.getInstance();
        if (++ticks > 3600) throw new IllegalStateException("JEI transfer timed out at stage " + stage);
        if (stage == 0) {
            if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) mc.setScreen(new TitleScreen());
            if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) return;
            mc.options.guiScale().set(2);
            mc.createWorldOpenFlows().createFreshLevel("tinker-jei-" + System.currentTimeMillis(),
                    new LevelSettings("JEI transfer", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42L, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            stage++;
        } else if ((stage == 1 || stage == 4) && mc.player != null && mc.screen == null && runtime != null) {
            var server = mc.getSingleplayerServer();
            var id = mc.player.getUUID();
            boolean useStorage = stage == 4;
            server.execute(() -> openFixture(server.getPlayerList().getPlayer(id), useStorage));
            fixtureTick = ticks;
            stage++;
        } else if (stage >= 2 && stage <= 6 && stage != 4) {
            check(ticks - fixtureTick < 200, "JEI transfer timed out at stage " + stage
                    + " with screen " + (mc.screen == null ? "closed" : mc.screen.getClass().getSimpleName()));
            if (!(mc.screen instanceof StorageScreenBase<?> screen)) return;
            var menu = screen.getMenu();
            var station = (TinkerStationUpgradeContainer) menu.getUpgradeContainers().get(0);
            if (station == null) return;
            var wrapper = station.getUpgradeWrapper();
            if (stage == 2 || stage == 5) {
                if (menu.getOpenContainer().orElse(null) != station) {
                    menu.setOpenTabId(station.getUpgradeContainerId());
                    return;
                }
                var before = wrapper.inventory().serializeNBT();
                if (stage == 2) {
                    if (menu.getSlot(0).getItem().isEmpty()) return;
                    check(transfer(menu, TConstructJEIConstants.MODIFIERS, unbreakable(), false, false) != null, "Five inputs cannot fit a basic station");
                    var recipe = runtime.getRecipeManager().createRecipeLookup(TConstructJEIConstants.TOOL_BUILDING).get()
                            .filter(candidate -> candidate.getId().getPath().equals("tools/building/pickaxe")).findFirst().orElseThrow();
                    requireTransfer(transfer(menu, TConstructJEIConstants.TOOL_BUILDING, recipe, false, false), "Building preflight");
                    check(before.equals(wrapper.inventory().serializeNBT()), "Building preflight is read-only");
                    requireTransfer(transfer(menu, TConstructJEIConstants.TOOL_BUILDING, recipe, false, true), "Building transfer request");
                } else {
                    if (wrapper.getTinkerableStack().isEmpty() || menu.getSlot(0).getItem().isEmpty()) return;
                    tool = wrapper.getTinkerableStack().copy();
                    var sparse = TinkerStationRecipeTransfer.preview(menu, station,
                            List.of(List.of(new ItemStack(Items.SHULKER_SHELL)), List.of(), List.of(new ItemStack(Items.DRAGON_BREATH))), false);
                    check(sparse.plan() != null && sparse.plan().ingredients().get(1).isEmpty()
                            && sparse.plan().ingredients().get(2).is(Items.DRAGON_BREATH), "A blank logical position stays blank");
                    requireTransfer(transfer(menu, TConstructJEIConstants.MODIFIERS, unbreakable(), true, false), "Modifier preflight");
                    check(before.equals(wrapper.inventory().serializeNBT()), "Modifier preflight is read-only");
                    requireTransfer(transfer(menu, TConstructJEIConstants.MODIFIERS, unbreakable(), true, true), "Modifier transfer request");
                }
                stage++;
            } else if (stage == 3 && !wrapper.result().isEmpty()) {
                check(wrapper.getTinkerableStack().isEmpty(), "Building keeps the tool slot empty");
                check(wrapper.getInput(0).is(TinkerToolParts.pickHead.get()) && wrapper.getInput(1).is(TinkerToolParts.toolHandle.get())
                        && wrapper.getInput(2).is(TinkerToolParts.toolBinding.get()), "Building filled the three ordered material positions");
                check(menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(1).getItem().is(Items.DIRT), "Storage supplied a part and recovered the old material");
                SophisticatedSmeltery.LOGGER.info("TINKER JEI BACKPACK BUILDING PASS");
                mc.player.closeContainer();
                stage++;
            } else if (stage == 6 && wrapper.getInput(4).getCount() == 3) {
                List<ItemStack> expected = List.of(new ItemStack(Items.SHULKER_SHELL, 3), new ItemStack(Items.DRAGON_BREATH, 3),
                        new ItemStack(Items.SHULKER_SHELL, 3), new ItemStack(Items.NETHERITE_INGOT, 3), new ItemStack(Items.NETHERITE_INGOT, 3));
                for (int i = 0; i < expected.size(); i++) check(ItemStack.matches(expected.get(i), wrapper.getInput(i)), "Ordered maximum modifier transfer");
                check(ItemStack.matches(tool, wrapper.getTinkerableStack()), "Modifier transfer preserves the tool and its NBT");
                check(mc.player.getOffhandItem().getCount() == 12, "Modifier transfer leaves offhand untouched");
                var otherStation = (TinkerStationUpgradeContainer) menu.getUpgradeContainers().get(1);
                check(otherStation.materialSlots().stream().noneMatch(slot -> slot.hasItem()), "Transfer targets only the open station");
                SophisticatedSmeltery.LOGGER.info("TINKER JEI TRANSFER PASS");
                stage = 8;
                mc.player.closeContainer();
                mc.stop();
            }
        }
    }

    private static IDisplayModifierRecipe unbreakable() {
        return runtime.getRecipeManager().createRecipeLookup(TConstructJEIConstants.MODIFIERS).get()
                .filter(recipe -> recipe.getRecipeId() != null && recipe.getRecipeId().getPath().equals("tools/modifiers/ability/unbreakable")
                        && recipe.getInputCount() == 5).findFirst().orElseThrow();
    }

    private static <R> IRecipeTransferError transfer(StorageContainerMenuBase<?> menu, RecipeType<R> type, R recipe, boolean max, boolean apply) {
        var recipes = runtime.getRecipeManager();
        var category = recipes.getRecipeCategory(type);
        var focus = runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
        var layout = recipes.createRecipeLayoutDrawable(category, recipe, focus).orElseThrow();
        var handler = runtime.getRecipeTransferManager().<StorageContainerMenuBase<?>, R>getRecipeTransferHandler(menu, category).orElseThrow();
        return handler.transferRecipe(menu, recipe, layout.getRecipeSlotsView(), Minecraft.getInstance().player, max, apply);
    }

    private static void openFixture(ServerPlayer player, boolean useStorage) {
        var pos = player.blockPosition().offset(2, 0, 0);
        IStorageWrapper storage;
        var backpack = new ItemStack(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.BACKPACK.get());
        var level = player.serverLevel();
        if (useStorage) {
            level.setBlockAndUpdate(pos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.NETHERITE_CHEST.get().defaultBlockState());
            storage = ((StorageBlockEntity)level.getBlockEntity(pos)).getStorageWrapper();
        } else {
            storage = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
        }
        var inventory = storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(useStorage
                ? SophisticatedSmeltery.STORAGE_TINKERS_ANVIL_UPGRADE.get() : SophisticatedSmeltery.TINKER_STATION_UPGRADE.get()));
        if (useStorage) {
            storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(SophisticatedSmeltery.STORAGE_TINKER_STATION_UPGRADE.get()));
        }
        var station = (TinkerStationUpgradeWrapper) storage.getUpgradeHandler().getSlotWrappers().get(0);
        var playerInventory = player.getInventory();
        if (useStorage) {
            inventory.setStackInSlot(0, new ItemStack(Items.SHULKER_SHELL, 6));
            inventory.setStackInSlot(1, new ItemStack(Items.DRAGON_BREATH, 5));
            playerInventory.setItem(11, new ItemStack(Items.NETHERITE_INGOT, 6));
            playerInventory.setItem(40, new ItemStack(Items.NETHERITE_INGOT, 12));
            station.inventory().setStackInSlot(0, ModifierWorktableUpgradeGameTests.tool());
        } else {
            var iron = MaterialVariantId.parse("tconstruct:iron");
            inventory.setStackInSlot(0, TinkerToolParts.pickHead.get().withMaterial(iron).copyWithCount(2));
            playerInventory.setItem(11, TinkerToolParts.toolHandle.get().withMaterial(iron));
            playerInventory.setItem(12, TinkerToolParts.toolBinding.get().withMaterial(iron));
            playerInventory.selected = 0;
            playerInventory.setItem(0, backpack);
            station.select(new ResourceLocation("tconstruct", "pickaxe"));
            station.setInput(0, new ItemStack(Items.DIRT));
        }
        player.inventoryMenu.broadcastChanges();
        storage.setOpenTabId(0);
        if (useStorage) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(level, pos));
            var update = level.getBlockEntity(pos).getUpdatePacket();
            if (update != null) player.connection.send(update);
            level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            check(player.containerMenu instanceof StorageContainerMenuBase<?>, "Storage fixture opened its menu");
        } else {
            backpack.getItem().use(level, player, InteractionHand.MAIN_HAND);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void requireTransfer(IRecipeTransferError error, String operation) {
        if (error != null) throw new IllegalStateException(operation + " failed with "
                + error.getTooltip().stream().map(net.minecraft.network.chat.Component::getString).toList());
    }
}
