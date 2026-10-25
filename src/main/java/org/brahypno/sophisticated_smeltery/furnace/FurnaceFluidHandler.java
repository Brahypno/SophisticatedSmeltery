package org.brahypno.sophisticated_smeltery.furnace;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;

/** Side-aware views can prefer installed fuel containers; other access prefers the furnace. */
public record FurnaceFluidHandler(MeltingUpgradeWrapper furnace, @Nullable IStorageFluidHandler next,
                                  ItemStack container, BooleanSupplier preferFuel) implements IStorageFluidHandler {
    public FurnaceFluidHandler(MeltingUpgradeWrapper furnace, @Nullable IStorageFluidHandler next, ItemStack container) {
        this(furnace, next, container, () -> false);
    }
    private int furnaceTanks() { return furnace.getTank().getTanks(); }
    @Override public ItemStack getContainer() { return container; }
    @Override public int getTanks() { return furnaceTanks() + (next == null ? 0 : next.getTanks()); }
    @Override public FluidStack getFluidInTank(int tank) {
        if (tank < 0) return FluidStack.EMPTY;
        return tank < furnaceTanks() ? furnace.getTank().getFluidInTank(tank).copy()
                : next == null ? FluidStack.EMPTY : next.getFluidInTank(tank - furnaceTanks());
    }
    @Override public int getTankCapacity(int tank) {
        if (tank < 0) return 0;
        return tank < furnaceTanks() ? furnace.getTank().getTankCapacity(tank)
                : next == null ? 0 : next.getTankCapacity(tank - furnaceTanks());
    }
    @Override public boolean isFluidValid(int tank, FluidStack fluid) {
        if (tank < 0) return false;
        return tank < furnaceTanks() ? furnace.getTank().isFluidValid(tank, fluid)
                : next != null && next.isFluidValid(tank - furnaceTanks(), fluid);
    }
    @Override public int fill(FluidStack fluid, FluidAction action) { return fill(fluid, action, false); }
    @Override public int fill(FluidStack fluid, FluidAction action, boolean ignoreInOutLimit) {
        if (preferFuel.getAsBoolean() && !fluid.isEmpty() && MeltingFuelLookup.findFuel(fluid.getFluid()) != null) {
            int filled = 0;
            for (var tank : furnace.getInternalFuelTanks()) {
                if (filled >= fluid.getAmount()) break;
                filled += tank.fill(new FluidStack(fluid, fluid.getAmount() - filled), action);
            }
            if (filled > 0 && action.execute()) furnace.persist();
            if (next != null && filled < fluid.getAmount()) {
                filled += next.fill(new FluidStack(fluid, fluid.getAmount() - filled), action, ignoreInOutLimit);
            }
            if (filled < fluid.getAmount()) {
                filled += fillFurnace(new FluidStack(fluid, fluid.getAmount() - filled), action);
            }
            return filled;
        }
        int filled = fillFurnace(fluid, action);
        if (next != null && filled < fluid.getAmount()) {
            FluidStack remainder = fluid.copy();
            remainder.shrink(filled);
            filled += next.fill(remainder, action, ignoreInOutLimit);
        }
        return filled;
    }
    private int fillFurnace(FluidStack fluid, FluidAction action) {
        int filled = furnace.getTank().fill(fluid, action);
        if (filled > 0 && action.execute()) furnace.persist();
        return filled;
    }
    @Override public FluidStack drain(int amount, FluidAction action) { return drain(amount, action, false); }
    @Override public FluidStack drain(int amount, FluidAction action, boolean ignoreInOutLimit) {
        FluidStack selected = furnace.getTank().drain(amount, FluidAction.SIMULATE);
        return selected.isEmpty() ? next == null ? FluidStack.EMPTY : next.drain(amount, action, ignoreInOutLimit)
                : drain(selected, action, ignoreInOutLimit);
    }
    @Override public FluidStack drain(FluidStack fluid, FluidAction action) { return drain(fluid, action, false); }
    @Override public FluidStack drain(FluidStack fluid, FluidAction action, boolean ignoreInOutLimit) {
        FluidStack drained = furnace.getTank().drain(fluid, action);
        if (!drained.isEmpty() && action.execute()) furnace.persist();
        if (next != null && drained.getAmount() < fluid.getAmount()) {
            FluidStack remaining = fluid.copy();
            remaining.shrink(drained.getAmount());
            FluidStack extra = next.drain(remaining, action, ignoreInOutLimit);
            if (drained.isEmpty()) return extra;
            drained.grow(extra.getAmount());
        }
        return drained;
    }
    @Override public FluidStack drain(TagKey<Fluid> tag, int amount, FluidAction action, boolean ignoreInOutLimit) {
        // Search the actual fluids, rather than limiting tag drains to the bottom fluid.
        for (FluidStack fluid : furnace.getTank().getFluids()) {
            if (fluid.getFluid().is(tag)) return drain(new FluidStack(fluid, amount), action, ignoreInOutLimit);
        }
        return next == null ? FluidStack.EMPTY : next.drain(tag, amount, action, ignoreInOutLimit);
    }
}
