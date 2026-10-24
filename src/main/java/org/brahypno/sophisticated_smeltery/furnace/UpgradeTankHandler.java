package org.brahypno.sophisticated_smeltery.furnace;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeWrapper;

/** Access the actual tank directly, keeping backpack void upgrades out of furnace transfers. */
public record UpgradeTankHandler(TankUpgradeWrapper tank) implements IFluidHandler {
    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int index) { return index == 0 ? tank.getContents() : FluidStack.EMPTY; }
    @Override public int getTankCapacity(int index) { return index == 0 ? tank.getTankCapacity() : 0; }
    @Override public boolean isFluidValid(int index, FluidStack fluid) {
        return index == 0 && (tank.getContents().isEmpty() || tank.getContents().isFluidEqual(fluid));
    }
    @Override public int fill(FluidStack fluid, FluidAction action) { return tank.fill(fluid, action, false); }
    @Override public FluidStack drain(int amount, FluidAction action) { return tank.drain(amount, action, false); }
    @Override public FluidStack drain(FluidStack fluid, FluidAction action) {
        return fluid.isFluidEqual(tank.getContents()) ? drain(fluid.getAmount(), action) : FluidStack.EMPTY;
    }
}
