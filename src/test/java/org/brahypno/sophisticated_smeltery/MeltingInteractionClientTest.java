package org.brahypno.sophisticated_smeltery;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import org.brahypno.sophisticated_smeltery.client.MeltingUpgradeTab;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeContainer;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiSmelteryTank;

/** Full screen clicks and real server responses; never modifies the client cursor or inventory directly. */
@Mod.EventBusSubscriber(modid = "sophisticatedsmeltery", value = Dist.CLIENT)
public final class MeltingInteractionClientTest {
    private static boolean running;
    private static int stage;
    private static int ticks;
    private static int frames;
    static void start() { running = true; }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (running && event.phase == TickEvent.Phase.END && ++ticks > 1800)
            throw new IllegalStateException("Actual fluid/bucket UI test timed out at stage " + stage);
    }

    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if (!running || !(event.getScreen() instanceof StorageScreenBase<?> screen) || ++frames < 20) return;
        var mc = Minecraft.getInstance();
        var container = screen.getMenu().getUpgradeContainers().values().stream().filter(MeltingUpgradeContainer.class::isInstance)
                .map(MeltingUpgradeContainer.class::cast).findFirst().orElseThrow();
        var wrapper = container.getUpgradeWrapper();
        var tab = screen.getUpgradeSettingsControl().children().stream().filter(MeltingUpgradeTab.class::isInstance)
                .map(MeltingUpgradeTab.class::cast).findFirst().orElseThrow();
        ItemStack carried = screen.getMenu().getCarried();
        var input = container.getSlots().get(9);
        var output = container.getSlots().get(10);
        Item gold = TinkerFluids.moltenGold.get().getBucket();
        Item iron = TinkerFluids.moltenIron.get().getBucket();
        switch (stage) {
            case 0 -> {
                for (var child : screen.getUpgradeSettingsControl().children()) {
                    if (child instanceof UpgradeSettingsTab<?> other && other != tab && other.getWidth() > 21)
                        click(screen, other.getX() + 8, other.getY() + 12, 0);
                }
                click(screen, tab.getX() + 8, tab.getY() + 12, 0);
            }
            case 1 -> {
                if (!container.isOpen()) return;
                clickFluid(screen, tab, container, gold, 0);
            }
            case 2 -> {
                if (wrapper.getTank().getFluidInTank(0).getFluid() != TinkerFluids.moltenGold.get()) return;
                clickSlot(screen, playerSlot(screen, 4));
            }
            case 3 -> {
                if (!carried.is(Items.BUCKET) || carried.getCount() != 8) return;
                clickFluid(screen, tab, container, gold, 0);
            }
            case 4 -> {
                if (!carried.is(Items.BUCKET) || carried.getCount() != 7 || wrapper.getTank().getContained() != 1000) return;
                check(countPlayer(screen, gold) == 1, "Left-clicking fluid must create exactly one filled bucket");
                clickSlot(screen, playerSlot(screen, 4));
            }
            // Native stack handling may put the filled bucket into the just-emptied hotbar slot.
            // Putting seven empty buckets back then swaps that filled bucket onto the cursor.
            case 5 -> { if (!carried.is(gold)) { if (!carried.isEmpty()) return; clickSlot(screen, findPlayerItem(screen, gold)); } }
            case 6 -> { if (!carried.is(gold)) return; click(screen, tab.getX() + 50, tab.getY() + 50, 1); }
            case 7 -> {
                if (!carried.is(Items.BUCKET) || wrapper.getTank().getContained() != 2000) return;
                clickSlot(screen, playerSlot(screen, 4));
            }
            case 8 -> { if (!carried.isEmpty()) return; clickFluid(screen, tab, container, gold, 0); clickMode(screen, tab); }
            case 9 -> { if (wrapper.getTransferDirection() != TransferDirection.EMPTY_ITEM) return; clickMode(screen, tab); }
            case 10 -> { if (wrapper.getTransferDirection() != TransferDirection.FILL_ITEM) return; clickSlot(screen, playerSlot(screen, 4)); }
            case 11 -> { if (!carried.is(Items.BUCKET) || carried.getCount() != 8) return; clickSlot(screen, input); }
            case 12 -> {
                if (!output.getItem().is(gold) || input.getItem().getCount() != 7 || !carried.isEmpty()) return;
                check(wrapper.getTank().getContained() == 1000, "GUI input/output fills exactly one selected bottom fluid bucket");
                capture(event, "bucket-ui-first-output");
                clickSlot(screen, output);
            }
            case 13 -> {
                if (!carried.is(gold) || !output.getItem().is(iron) || input.getItem().getCount() != 6) return;
                check(wrapper.getTank().getContained() == 0, "Taking GUI output fills the next waiting bucket from the new bottom fluid");
                clickSlot(screen, playerSlot(screen, 5));
            }
            case 14 -> { if (!carried.isEmpty()) return; clickSlot(screen, output); }
            case 15 -> { if (!carried.is(iron)) return; clickSlot(screen, playerSlot(screen, 6)); }
            case 16 -> { if (!carried.isEmpty() || !output.getItem().isEmpty()) return; clickSlot(screen, input); }
            case 17 -> { if (!carried.is(Items.BUCKET) || carried.getCount() != 6) return; clickSlot(screen, playerSlot(screen, 4)); }
            case 18 -> { if (!carried.isEmpty()) return; clickMode(screen, tab); }
            case 19 -> { if (wrapper.getTransferDirection() != TransferDirection.AUTO) return; clickMode(screen, tab); }
            case 20 -> { if (wrapper.getTransferDirection() != TransferDirection.EMPTY_ITEM) return; clickSlot(screen, playerSlot(screen, 5)); }
            case 21 -> { if (!carried.is(gold)) return; clickSlot(screen, input); }
            case 22 -> {
                if (!output.getItem().is(Items.BUCKET) || !input.getItem().isEmpty() || wrapper.getTank().getContained() != 1000) return;
                clickSlot(screen, output);
            }
            case 23 -> { if (!carried.is(Items.BUCKET)) return; clickSlot(screen, playerSlot(screen, 4)); }
            case 24 -> {
                if (!carried.isEmpty()) return;
                check(countPlayer(screen, Items.BUCKET) == 7 && countPlayer(screen, iron) == 1 && countPlayer(screen, gold) == 0,
                        "All eight original containers must survive the actual click sequence");
                check(input.getItem().isEmpty() && output.getItem().isEmpty()
                        && wrapper.getTank().getFluidInTank(0).getFluid() == TinkerFluids.moltenGold.get(),
                        "GUI injection leaves the exact expected gold fluid and no hidden container items");
                capture(event, "bucket-ui-injection-complete");
                SophisticatedSmeltery.LOGGER.info("ACTUAL FLUID UI PASSED: layer selection, stacked cursor fill, right-click injection, all three modes, GUI slots, consecutive output and exact conservation");
                running = false;
                mc.player.closeContainer();
                mc.stop();
            }
            default -> throw new IllegalStateException("Unexpected test stage " + stage);
        }
        SophisticatedSmeltery.LOGGER.info("Fluid UI click stage {} completed", stage);
        stage++;
        frames = 0;
    }

    private static void clickFluid(StorageScreenBase<?> screen, MeltingUpgradeTab tab, MeltingUpgradeContainer container, Item bucket, int button) {
        var tank = new GuiSmelteryTank(screen, container.getUpgradeWrapper().getTank(),
                tab.getX() - screen.getGuiLeft() + 5, tab.getY() - screen.getGuiTop() + 24, 106, 106,
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(container.getUpgradeWrapper().getFurnace().controller().getType()));
        for (int y = tab.getY() + 25; y < tab.getY() + 130; y++) {
            int index = tank.getFluidClicked(tab.getX() + 50 - screen.getGuiLeft(), y - screen.getGuiTop());
            if (index >= 0 && container.getUpgradeWrapper().getTank().getFluidInTank(index).getFluid().getBucket() == bucket) {
                click(screen, tab.getX() + 50, y, button);
                return;
            }
        }
        throw new IllegalStateException("Expected fluid layer is not visible/clickable");
    }
    private static void clickMode(StorageScreenBase<?> screen, MeltingUpgradeTab tab) { click(screen, tab.getX() + 91, tab.getY() + 172, 0); }
    private static Slot playerSlot(StorageScreenBase<?> screen, int index) {
        return screen.getMenu().slots.stream().filter(slot -> slot.container == Minecraft.getInstance().player.getInventory()
                && slot.getSlotIndex() == index).findFirst().orElseThrow();
    }
    private static Slot findPlayerItem(StorageScreenBase<?> screen, Item item) {
        return screen.getMenu().slots.stream().filter(slot -> slot.container == Minecraft.getInstance().player.getInventory()
                && slot.getItem().is(item)).findFirst().orElseThrow();
    }
    private static int countPlayer(StorageScreenBase<?> screen, Item item) {
        return screen.getMenu().slots.stream().filter(slot -> slot.container == Minecraft.getInstance().player.getInventory()
                && slot.getItem().is(item)).mapToInt(slot -> slot.getItem().getCount()).sum();
    }
    private static void clickSlot(StorageScreenBase<?> screen, Slot slot) { click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8, 0); }
    private static void click(StorageScreenBase<?> screen, int x, int y, int button) {
        check(screen.mouseClicked(x, y, button), "Normal screen click must be dispatched");
        screen.mouseReleased(x, y, button);
    }
    private static void capture(ScreenEvent.Render.Post event, String name) {
        event.getGuiGraphics().flush();
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) { image.writeToFile(Path.of("..", name + ".png")); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
