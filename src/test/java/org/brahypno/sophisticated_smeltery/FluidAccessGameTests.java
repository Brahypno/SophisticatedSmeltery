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
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import org.brahypno.sophisticated_smeltery.furnace.BackpackFluidInteraction;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.furnace.UpgradeTankHandler;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;

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
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
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

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void fuelInputPrefersInstalledContainersAndOverflowsIntoFurnace(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.NETHERITE_BACKPACK.get());
        var block = (BackpackBlockEntity)helper.getBlockEntity(pos);
        block.setBackpack(backpack().getBackpack());
        var storage = (BackpackWrapper)block.getBackpackWrapper();
        var furnace = melting(storage);
        furnace.getComponents().setStackInSlot(1,
                new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK)));
        var internal = furnace.getInternalFuelTanks().get(0);
        int total = 0;
        int furnaceTotal = 0;
        var handlers = new java.util.EnumMap<Direction, IFluidHandler>(Direction.class);
        for (Direction side : Direction.values()) {
            handlers.put(side, block.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(IllegalStateException::new));
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            helper.setBlock(pos, block.getBlockState().setValue(BackpackBlock.FACING, facing));
            for (Direction side : Direction.values()) {
                var handler = handlers.get(side);
                var before = furnace.getUpgradeStack().copy();
                helper.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 50), IFluidHandler.FluidAction.SIMULATE) == 50
                                && internal.getFluidInTank(0).getAmount() == total && furnace.getTank().getContained() == furnaceTotal
                                && ItemStack.matches(before, furnace.getUpgradeStack()),
                        "Simulation leaves both destinations unchanged on " + side + " facing " + facing);
                helper.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 50), IFluidHandler.FluidAction.EXECUTE) == 50,
                        "All sides still accept fuel");
                if (side == facing.getClockWise() || side == facing.getCounterClockWise()) total += 50;
                else furnaceTotal += 50;
                helper.assertTrue(internal.getFluidInTank(0).getAmount() == total && furnace.getTank().getContained() == furnaceTotal,
                        "Only left and right prefer fuel containers, including cached capabilities after rotation: " + side + " facing " + facing);
            }
        }
        var restored = new MeltingUpgradeWrapper(storage, furnace.getUpgradeStack().copy(), ignored -> {});
        helper.assertTrue(restored.getInternalFuelTanks().get(0).getFluidInTank(0).getAmount() == total,
                "External input saves internal tank contents immediately");
        var unsidedSource = new FluidTank(50);
        unsidedSource.fill(new FluidStack(Fluids.LAVA, 50), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(BackpackFluidInteraction.transfer(storage, unsidedSource, true) == 50
                        && internal.getFluidInTank(0).getAmount() == total && furnace.getTank().getContained() == furnaceTotal + 50,
                "Held-backpack transfers retain furnace priority without a backpack face");
        var unsided = block.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElseThrow(IllegalStateException::new);
        helper.assertTrue(unsided.fill(new FluidStack(Fluids.LAVA, 50), IFluidHandler.FluidAction.EXECUTE) == 50
                        && internal.getFluidInTank(0).getAmount() == total && furnace.getTank().getContained() == furnaceTotal + 100,
                "Unsided capability retains furnace priority");
        helper.assertTrue(unsided.drain(furnaceTotal + 100, IFluidHandler.FluidAction.EXECUTE).getAmount() == furnaceTotal + 100
                        && internal.getFluidInTank(0).getAmount() == total,
                "Extraction still prefers furnace and leaves internal fuel intact");

        var oldCapability = block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST);
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
        helper.assertTrue(!oldCapability.isPresent(), "Side capabilities invalidate when upgrades change");
        furnace = melting(storage);
        internal = furnace.getInternalFuelTanks().get(0);
        var ordinary = (UpgradeTankHandler)furnace.getBackpackTanks().get(0);
        internal.fill(new FluidStack(Fluids.LAVA, internal.getTankCapacity(0) - total - 100), IFluidHandler.FluidAction.EXECUTE);
        ordinary.tank().fill(new FluidStack(Fluids.LAVA, ordinary.getTankCapacity(0) - 200), IFluidHandler.FluidAction.EXECUTE, true);
        var lateral = block.getBlockState().getValue(BackpackBlock.FACING).getClockWise();
        var handler = block.getCapability(ForgeCapabilities.FLUID_HANDLER, lateral).orElseThrow(IllegalStateException::new);
        var before = furnace.getUpgradeStack().copy();
        helper.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.SIMULATE) == 1000
                        && internal.getFluidInTank(0).getAmount() == internal.getTankCapacity(0) - 100
                        && ordinary.getFluidInTank(0).getAmount() == ordinary.getTankCapacity(0) - 200
                        && furnace.getTank().getContained() == 0 && ItemStack.matches(before, furnace.getUpgradeStack()),
                "Simulated fuel input checks all destinations without changing contents or NBT");
        var source = new FluidTank(1000);
        source.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(FluidUtil.tryFluidTransfer(handler, source, 1000, true).getAmount() == 1000 && source.isEmpty()
                        && internal.getFluidInTank(0).getAmount() == internal.getTankCapacity(0)
                        && ordinary.getFluidInTank(0).getAmount() == ordinary.getTankCapacity(0)
                        && furnace.getTank().getContained() == 700,
                "Lateral transfer fills internal tank, backpack tank, then furnace with the remainder");

        // An incompatible installed tank must not prevent furnace fallback.
        storage.getUpgradeHandler().setStackInSlot(1, ItemStack.EMPTY);
        furnace = melting(storage);
        internal = furnace.getInternalFuelTanks().get(0);
        internal.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        internal.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
        handler = block.getCapability(ForgeCapabilities.FLUID_HANDLER, lateral).orElseThrow(IllegalStateException::new);
        helper.assertTrue(handler.fill(new FluidStack(TinkerFluids.blazingBlood.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 100
                        && internal.getFluidInTank(0).getFluid() == Fluids.WATER && furnace.getTank().getContained() == 800,
                "Recognize other native fuel recipes and fall back past incompatible containers");
        furnace.getComponents().setStackInSlot(1, ItemStack.EMPTY);
        helper.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 100
                        && furnace.getTank().getContained() == 900,
                "Fuel still enters furnace when no fuel containers are installed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
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
