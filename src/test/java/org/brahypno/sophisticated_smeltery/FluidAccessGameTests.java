package org.brahypno.sophisticated_smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import org.brahypno.sophisticated_smeltery.furnace.BackpackFluidInteraction;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

@GameTestHolder("sophisticatedsmeltery")
@PrefixGameTestTemplate(false)
public final class FluidAccessGameTests {
    private static BackpackWrapper backpack() {
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, ControllerBinding.bind(
                new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), new ItemStack(TinkerSmeltery.smelteryController)));
        melting(storage).getComponents().setStackInSlot(0, new ItemStack(TinkerSmeltery.searedBricks, 32));
        return storage;
    }
    private static MeltingUpgradeWrapper melting(BackpackWrapper storage) {
        return (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
    }
    private static FluidStack iron(int amount) { return new FluidStack(TinkerFluids.moltenIron.get(), amount); }
    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void placedFluidCapabilitySimulationPriorityAndLifecycle(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.NETHERITE_BACKPACK.get());
        var block = (BackpackBlockEntity)helper.getBlockEntity(pos);
        var blank = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        blank.getInventoryHandler();
        block.setBackpack(blank.getBackpack());
        var initial = block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH);
        helper.assertTrue(initial.orElseThrow(IllegalStateException::new).fill(iron(100), IFluidHandler.FluidAction.SIMULATE) == 0,
                "Empty backpack has no fluid capacity");
        var installed = block.getBackpackWrapper();
        installed.getUpgradeHandler().setStackInSlot(0, ControllerBinding.bind(
                new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), new ItemStack(TinkerSmeltery.smelteryController)));
        ((MeltingUpgradeWrapper)installed.getUpgradeHandler().getSlotWrappers().get(0)).getComponents()
                .setStackInSlot(0, new ItemStack(TinkerSmeltery.searedBricks, 32));
        var storage = (BackpackWrapper)installed;
        var handler = block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElseThrow(IllegalStateException::new);
        var furnace = melting(storage);
        var before = furnace.getUpgradeStack().copy();
        helper.assertTrue(handler.fill(iron(1000), IFluidHandler.FluidAction.SIMULATE) == 1000
                && furnace.getTank().getContained() == 0 && ItemStack.matches(before, furnace.getUpgradeStack()),
                "Simulation must neither store nor save fluid");
        helper.assertTrue(handler.fill(iron(1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "Expose furnace without a tank upgrade");
        var reloaded = new MeltingUpgradeWrapper(storage, furnace.getUpgradeStack().copy(), stack -> {});
        helper.assertTrue(reloaded.getTank().getContained() == 1000, "Pipe input must save without waiting for a tick");
        int listed = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) listed += handler.getFluidInTank(tank).getAmount();
        helper.assertTrue(listed == 1000, "Advertise actual furnace contents to other mods");
        helper.assertTrue(handler.drain(iron(400), IFluidHandler.FluidAction.SIMULATE).getAmount() == 400
                && furnace.getTank().getContained() == 1000, "Simulated extraction must leave fluid intact");
        helper.assertTrue(handler.drain(iron(400), IFluidHandler.FluidAction.EXECUTE).getAmount() == 400, "Typed extraction must work");
        reloaded = new MeltingUpgradeWrapper(storage, furnace.getUpgradeStack().copy(), stack -> {});
        helper.assertTrue(reloaded.getTank().getContained() == 600, "Pipe output must save immediately");

        // Preserve furnace priority when an ordinary tank is added and wrappers are rebuilt.
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
        furnace = melting(storage);
        handler = storage.getFluidHandler().orElseThrow();
        var ordinary = furnace.getBackpackTanks().get(0);
        ordinary.fill(iron(500), IFluidHandler.FluidAction.EXECUTE);
        handler.fill(iron(300), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(furnace.getTank().getContained() == 900 && ordinary.getFluidInTank(0).getAmount() == 500,
                "Input prefers furnace over ordinary tank");
        helper.assertTrue(handler.drain(200, IFluidHandler.FluidAction.EXECUTE).getAmount() == 200
                && furnace.getTank().getContained() == 700 && ordinary.getFluidInTank(0).getAmount() == 500,
                "Untyped output prefers furnace");
        helper.assertTrue(handler.drain(iron(1000), IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000
                && furnace.getTank().getContained() == 0 && ordinary.getFluidInTank(0).getAmount() == 200,
                "Typed extraction continues into ordinary tanks without mixing");
        handler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(storage.getFluidHandler().orElseThrow().drain(FluidTags.WATER, 100, IFluidHandler.FluidAction.EXECUTE, false).getAmount() == 100,
                "Tag-based pumps can find matching fluid");

        handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        storage.getUpgradeHandler().setStackInSlot(1, ItemStack.EMPTY);
        for (Direction side : Direction.values()) {
            handler = block.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(IllegalStateException::new);
            helper.assertTrue(handler.fill(iron(100), IFluidHandler.FluidAction.EXECUTE) == 100,
                    "Standard pipe input must access furnace on " + side);
        }
        handler = block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElseThrow(IllegalStateException::new);
        helper.assertTrue(handler.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 600, "All sides access the same stored fluid");
        block.getBackpackWrapper().getUpgradeHandler().setStackInSlot(0, ItemStack.EMPTY);
        helper.assertTrue(block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                .orElseThrow(IllegalStateException::new).fill(iron(100), IFluidHandler.FluidAction.SIMULATE) == 0,
                "Removing controller must invalidate pipe access");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "sophisticatedsmeltery")
    public static void rightClickChoosesDirectionAndConsumesBlockedTransfers(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.NETHERITE_BACKPACK.get());
        var target = (BackpackBlockEntity)helper.getBlockEntity(pos);
        var targetStorage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        targetStorage.getInventoryHandler();
        targetStorage.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.TANK_UPGRADE.get()));
        target.setBackpack(targetStorage.getBackpack());
        var player = helper.makeMockPlayer();
        var source = backpack();
        player.setItemInHand(InteractionHand.MAIN_HAND, source.getBackpack());
        var absolute = helper.absolutePos(pos);
        var click = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, absolute,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
        var targetHandler = target.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElseThrow(IllegalStateException::new);
        targetHandler.fill(iron(500), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(BackpackFluidInteraction.transfer(source, targetHandler, false) == 500
                && melting(source).getTank().getContained() == 0
                && targetHandler.drain(1000, IFluidHandler.FluidAction.SIMULATE).getAmount() == 500, "Preview cannot transfer");
        MinecraftForge.EVENT_BUS.post(click);
        var held = player.getMainHandItem().getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
        helper.assertTrue(click.isCanceled() && held.getFluidHandler().orElseThrow()
                .drain(1000, IFluidHandler.FluidAction.SIMULATE).getAmount() == 500,
                "Normal right click must consume interaction and draw clicked tank into held furnace");
        helper.assertTrue(targetHandler.drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "Source tank drained exactly once");

        var emptyClick = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, absolute,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
        MinecraftForge.EVENT_BUS.post(emptyClick);
        helper.assertTrue(emptyClick.isCanceled() && held.getFluidHandler().orElseThrow().drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty()
                && targetHandler.drain(1000, IFluidHandler.FluidAction.SIMULATE).getAmount() == 500,
                "Empty target reverses transfer through the real event");
        var furnace = melting((BackpackWrapper)held);
        furnace.getTank().fill(iron(furnace.getTank().getCapacity()), IFluidHandler.FluidAction.EXECUTE);
        var blockedClick = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, absolute,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
        MinecraftForge.EVENT_BUS.post(blockedClick);
        helper.assertTrue(blockedClick.isCanceled()
                && targetHandler.drain(1000, IFluidHandler.FluidAction.SIMULATE).getAmount() == 500,
                "Full destination still consumes right click without reversing or placing a backpack");
        helper.succeed();
    }
}
