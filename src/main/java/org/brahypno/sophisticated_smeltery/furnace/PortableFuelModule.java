package org.brahypno.sophisticated_smeltery.furnace;

import java.util.List;
import java.util.ArrayList;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.nbt.CompoundTag;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MultitankFuelModule;

/** Reuses native fuel recipes and burn accounting without looking up world tanks. */
public final class PortableFuelModule extends MultitankFuelModule {
    private final Supplier<List<IFluidHandler>> sources;
    private final IntUnaryOperator temperatureDemand;
    private FluidStack lastFuel = FluidStack.EMPTY;

    public PortableFuelModule(MantleBlockEntity parent, Supplier<List<IFluidHandler>> sources) {
        this(parent, sources, available -> 0);
    }

    public PortableFuelModule(MantleBlockEntity parent, Supplier<List<IFluidHandler>> sources, IntUnaryOperator temperatureDemand) {
        super(parent, List::of);
        this.sources = sources;
        this.temperatureDemand = temperatureDemand;
    }

    public boolean hasSources() { return !sources.get().isEmpty(); }

    @Override public FuelInfo getFuelInfo() {
        List<IFluidHandler> available = sources.get();
        if (!lastFuel.isEmpty()) {
            for (IFluidHandler source : available) {
                FluidStack fluid = source.getFluidInTank(0);
                if (!fluid.isEmpty() && fluid.isFluidEqual(lastFuel)) {
                    MeltingFuel recipe = findRecipe(fluid.getFluid());
                    return FuelInfo.of(fluid, source.getTankCapacity(0), recipe == null ? 0 : recipe.getTemperature());
                }
            }
        }
        for (IFluidHandler source : available) {
            for (int tank = 0; tank < source.getTanks(); tank++) {
                FluidStack fluid = source.getFluidInTank(tank);
                if (!fluid.isEmpty()) {
                    MeltingFuel recipe = findRecipe(fluid.getFluid());
                    return FuelInfo.of(fluid, source.getTankCapacity(tank), recipe == null ? 0 : recipe.getTemperature());
                }
            }
        }
        return FuelInfo.EMPTY;
    }

    @Override public FluidStack getLastFluid() { return getFuelInfo().getFluid(); }
    @Override public int getTanks() { return sources.get().stream().mapToInt(IFluidHandler::getTanks).sum(); }
    private record Route(IFluidHandler handler, int tank) { }
    private Route route(int index) {
        if (index < 0) return null;
        for (IFluidHandler source : sources.get()) {
            if (index < source.getTanks()) return new Route(source, index);
            index -= source.getTanks();
        }
        return null;
    }
    @Override public FluidStack getFluidInTank(int tank) {
        Route route = route(tank);
        return route == null ? FluidStack.EMPTY : route.handler.getFluidInTank(route.tank);
    }
    @Override public int getTankCapacity(int tank) {
        Route route = route(tank);
        return route == null ? 0 : route.handler.getTankCapacity(route.tank);
    }
    @Override public boolean isFluidValid(int tank, FluidStack fluid) {
        Route route = route(tank);
        return route != null && route.handler.isFluidValid(route.tank, fluid);
    }
    @Override public int fill(FluidStack fluid, FluidAction action) {
        int total = 0;
        for (IFluidHandler source : sources.get()) {
            int filled = source.fill(new FluidStack(fluid, fluid.getAmount() - total), action);
            total += filled;
            if (total >= fluid.getAmount()) break;
        }
        if (action.execute() && total > 0) parent.setChangedFast();
        return total;
    }
    @Override public FluidStack drain(FluidStack fluid, FluidAction action) {
        FluidStack result = FluidStack.EMPTY;
        int remaining = fluid.getAmount();
        for (IFluidHandler source : sources.get()) {
            FluidStack drained = source.drain(new FluidStack(fluid, remaining), action);
            if (!drained.isEmpty()) {
                if (result.isEmpty()) result = drained;
                else result.grow(drained.getAmount());
                remaining -= drained.getAmount();
                if (remaining <= 0) break;
            }
        }
        if (action.execute() && !result.isEmpty()) parent.setChangedFast();
        return result;
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        FluidStack fluid = getLastFluid();
        return fluid.isEmpty() ? FluidStack.EMPTY : drain(new FluidStack(fluid, amount), action);
    }

    // The native shared lookup is cached and invalidated on datapack reload.
    // Avoid retaining an obsolete recipe in the detached fuel module.
    @Override protected MeltingFuel findRecipe(Fluid fluid) {
        return MeltingFuelLookup.findFuel(fluid);
    }

    @Override public int findFuel(boolean consume) {
        record Candidate(IFluidHandler source, int temperature) { }
        List<Candidate> available = new ArrayList<>();
        int maximum = 0;
        for (IFluidHandler source : sources.get()) {
            FluidStack fluid = source.getFluidInTank(0);
            MeltingFuel recipe = findRecipe(fluid.getFluid());
            if (recipe != null && fluid.getAmount() >= recipe.getAmount(fluid.getFluid())) {
                available.add(new Candidate(source, recipe.getTemperature()));
                maximum = Math.max(maximum, recipe.getTemperature());
            }
        }
        int required = temperatureDemand.applyAsInt(maximum);
        Candidate best = null;
        for (Candidate candidate : available) {
            if (candidate.temperature >= required && (best == null || candidate.temperature < best.temperature)) best = candidate;
        }
        // Stable ties retain source order: internal Tinkers tanks precede backpack tanks.
        if (best != null) {
            FluidStack selected = best.source.getFluidInTank(0).copy();
            int temperature = tryLiquidFuel(best.source, consume);
            if (consume && temperature > 0) lastFuel = selected;
            return temperature;
        }
        if (consume) { temperature = 0; rate = 0; }
        return 0;
    }

    @Override public CompoundTag writeToTag(CompoundTag tag) {
        super.writeToTag(tag);
        tag.putInt("quality", fuelQuality);
        tag.put("lastFuel", lastFuel.writeToNBT(new CompoundTag()));
        return tag;
    }
    @Override public void readFromTag(CompoundTag tag) {
        super.readFromTag(tag);
        fuelQuality = tag.getInt("quality");
        lastFuel = FluidStack.loadFluidStackFromNBT(tag.getCompound("lastFuel"));
    }
}
