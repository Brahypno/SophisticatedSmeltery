package org.brahypno.sophisticated_smeltery.furnace;

import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeItem;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeItem;

public final class BackpackFluidInteraction {
    private BackpackFluidInteraction() {}

    public static int transfer(IStorageWrapper storage, IFluidHandler target, boolean execute) {
        IFluidHandler backpack = storage.getFluidHandler().orElse(null);
        if (backpack == null) return 0;
        boolean hasFluid = false;
        for (int tank = 0; tank < target.getTanks(); tank++) {
            if (!target.getFluidInTank(tank).isEmpty()) { hasFluid = true; break; }
        }
        return (hasFluid ? FluidUtil.tryFluidTransfer(backpack, target, Integer.MAX_VALUE, execute)
                : FluidUtil.tryFluidTransfer(target, backpack, Integer.MAX_VALUE, execute)).getAmount();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof BackpackItem)) return;
        var storage = event.getItemStack().getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElse(null);
        if (storage == null || storage.getRenderInfo().getUpgradeItems().stream()
                .noneMatch(stack -> stack.getItem() instanceof MeltingUpgradeItem || stack.getItem() instanceof AlloyerUpgradeItem)) return;
        var block = event.getLevel().getBlockEntity(event.getPos());
        if (block == null) return;
        IFluidHandler target = block.getCapability(ForgeCapabilities.FLUID_HANDLER, event.getFace()).orElse(null);
        if (target == null) return;
        if (!event.getLevel().isClientSide) transfer(storage, target, true);
        // Consume even a blocked transfer so a full tank does not place the backpack.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }
}
