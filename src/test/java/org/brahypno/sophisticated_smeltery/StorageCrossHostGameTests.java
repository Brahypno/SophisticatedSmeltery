package org.brahypno.sophisticated_smeltery;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageEndpointAdapter;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointAdapter.Compatibility;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkedStorageEndpointAdapter;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import slimeknights.tconstruct.fluids.TinkerFluids;

@PrefixGameTestTemplate(false)
public final class StorageCrossHostGameTests {
    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void backpackAndStorageShareFluidsWithEitherCanonicalHost(GameTestHelper helper) {
        for (boolean backpackRoot : new boolean[]{true, false}) {
            var pos = new BlockPos(backpackRoot ? 1 : 3, 1, 1);
            helper.setBlock(pos, ModBlocks.NETHERITE_CHEST.get());
            var block = (StorageBlockEntity)helper.getBlockEntity(pos);
            var stack = new ItemStack(ModItems.NETHERITE_BACKPACK.get());
            var backpack = new BackpackWrapper(stack);
            backpack.getInventoryHandler();
            IStorageWrapper root = backpackRoot ? backpack : block.getStorageWrapper();
            var furnace = StorageCompatibilityGameTests.install(root);
            furnace.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 1000), FluidAction.EXECUTE);
            furnace.persist();
            var backpackAdapter = new BackpackLinkedStorageEndpointAdapter();
            var storageAdapter = new StorageLinkedStorageEndpointAdapter();
            var level = helper.getLevel();
            var descriptor = backpackRoot ? backpackAdapter.createHostDescriptor(level, stack) : storageAdapter.createHostDescriptor(level, block);
            var contents = backpackRoot ? backpackAdapter.copyCanonicalContents(level, stack) : storageAdapter.copyCanonicalContents(level, block);
            helper.assertTrue((backpackRoot ? storageAdapter.getCompatibility(level, block, descriptor)
                    : backpackAdapter.getCompatibility(level, stack, descriptor)) == Compatibility.COMPATIBLE, "Native linking accepts the opposite empty host");
            var manager = LinkedStorageGroupsSavedData.get(level).manager();
            var endpoint = UUID.randomUUID();
            var group = manager.createGroup(UUID.randomUUID(), endpoint, descriptor, contents);
            var primary = new LinkedStorageEndpointData(group, endpoint);
            var secondary = manager.createSecondaryEndpoint(primary).orElseThrow();
            backpackAdapter.bindEndpoint(level, stack, backpackRoot ? primary : secondary);
            storageAdapter.bindEndpoint(level, block, backpackRoot ? secondary : primary);
            var backFluid = BackpackLinkedStorageResolver.resolve(level, stack).orElseThrow().getFluidHandler().orElseThrow();
            helper.assertTrue(backFluid == block.getStorageWrapper().getFluidHandler().orElseThrow(), "Mixed endpoints share the canonical fluid handler");
            var pipe = block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElseThrow(IllegalStateException::new);
            helper.assertTrue(pipe.drain(300, FluidAction.EXECUTE).getAmount() == 300, "Storage pipe drains a mixed group");
            helper.assertTrue(backFluid.fill(new FluidStack(TinkerFluids.moltenIron.get(), 200), FluidAction.EXECUTE) == 200
                    && pipe.drain(2000, FluidAction.EXECUTE).getAmount() == 900, "Backpack interaction and storage pipes conserve the same fluid");
        }
        helper.succeed();
    }
}
