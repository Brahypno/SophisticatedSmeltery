package org.brahypno.sophisticated_smeltery.furnace;

import java.util.List;
import java.util.ArrayList;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MultitankFuelModule;

/** Reuses native fuel recipes and burn accounting without looking up world tanks. */
public final class PortableFuelModule extends MultitankFuelModule {
    private final Supplier<List<IFluidHandler>> sources;
    private final IntUnaryOperator temperatureDemand;
    private Supplier<ItemStack> solidSource = () -> ItemStack.EMPTY;
    private Consumer<ItemStack> solidRemainder = ignored -> {};
    private FluidStack lastFuel = FluidStack.EMPTY;
    private final List<Burn> pausedBurns = new ArrayList<>();

    /** Already-paid burn time keeps its original temperature, speed and quality. */
    private record Burn(int remaining, int temperature, int rate, int quality, FluidStack fluid) {
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("fuel", remaining);
            tag.putInt("temperature", temperature);
            tag.putInt("rate", rate);
            tag.putInt("quality", quality);
            tag.put("lastFuel", fluid.writeToNBT(new CompoundTag()));
            return tag;
        }
        private static Burn load(CompoundTag tag) {
            return new Burn(tag.getInt("fuel"), tag.getInt("temperature"), tag.getInt("rate"),
                    tag.getInt("quality"), FluidStack.loadFluidStackFromNBT(tag.getCompound("lastFuel")));
        }
    }
    private record Candidate(IFluidHandler source, Burn saved, int temperature) { }
    private record Selection(Candidate best, int required) { }

    private Burn currentBurn() { return new Burn(fuel, temperature, rate, fuelQuality, lastFuel.copy()); }
    private void restore(Burn burn) {
        fuel = burn.remaining;
        temperature = burn.temperature;
        rate = burn.rate;
        fuelQuality = burn.quality;
        lastFuel = burn.fluid.copy();
    }
    private void clearBurn() {
        fuel = 0;
        temperature = 0;
        rate = 0;
        fuelQuality = 0;
        lastFuel = FluidStack.EMPTY;
    }

    /** Let the actual controller decide whether to ignite after changing temperature demand. */
    void heatWithFuelSelection(Runnable nativeHeat) {
        if (!hasFuel()) { nativeHeat.run(); return; }
        Selection selection = selectFuel(true);
        if (selection.required <= 0 || selection.best == null || selection.best.temperature == temperature) {
            nativeHeat.run();
            return;
        }
        Burn previous = currentBurn();
        pausedBurns.add(previous);
        clearBurn();
        nativeHeat.run();
        // A custom controller may decline ignition (for example its alloy lever is off).
        if (!hasFuel()) {
            pausedBurns.remove(previous);
            restore(previous);
        }
    }

    public PortableFuelModule(MantleBlockEntity parent, Supplier<List<IFluidHandler>> sources) {
        this(parent, sources, available -> 0);
    }

    public PortableFuelModule(MantleBlockEntity parent, Supplier<List<IFluidHandler>> sources, IntUnaryOperator temperatureDemand) {
        super(parent, List::of);
        this.sources = sources;
        this.temperatureDemand = temperatureDemand;
    }

    public boolean hasSources() { return !sources.get().isEmpty() || solidTime() > 0; }
    public void setSolidSource(Supplier<ItemStack> source, Consumer<ItemStack> remainder) {
        solidSource = source;
        solidRemainder = remainder;
    }
    private int solidTime() { return ForgeHooks.getBurnTime(solidSource.get(), TinkerRecipeTypes.FUEL.get()) / 4; }

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
        return solidTime() > 0 ? FuelInfo.ITEM : FuelInfo.EMPTY;
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

    private Selection selectFuel(boolean includeCurrent) {
        List<Candidate> available = new ArrayList<>();
        int maximum = 0;
        if (includeCurrent && hasFuel()) {
            available.add(new Candidate(null, currentBurn(), temperature));
            maximum = temperature;
        }
        for (Burn burn : pausedBurns) {
            available.add(new Candidate(null, burn, burn.temperature));
            maximum = Math.max(maximum, burn.temperature);
        }
        for (IFluidHandler source : sources.get()) {
            FluidStack fluid = source.getFluidInTank(0);
            MeltingFuel recipe = findRecipe(fluid.getFluid());
            if (recipe != null && fluid.getAmount() >= recipe.getAmount(fluid.getFluid())) {
                available.add(new Candidate(source, null, recipe.getTemperature()));
                maximum = Math.max(maximum, recipe.getTemperature());
            }
        }
        int solidTemperature = solidTime() > 0 ? MeltingFuelLookup.getSolid().getTemperature() : 0;
        int required = temperatureDemand.applyAsInt(Math.max(maximum, solidTemperature));
        Candidate best = null;
        for (Candidate candidate : available) {
            if (candidate.temperature >= required && (best == null || candidate.temperature < best.temperature)) best = candidate;
        }
        // Keep the native liquid-before-solid priority. A null source and burn denotes fresh solid fuel.
        if (best == null && solidTemperature > 0 && solidTemperature >= required) best = new Candidate(null, null, solidTemperature);
        return new Selection(best, required);
    }

    @Override public int findFuel(boolean consume) {
        Candidate best = selectFuel(false).best;
        // Reuse paid burn time first; fresh-fluid ties retain internal-before-backpack order.
        if (best != null) {
            if (!consume) return best.temperature;
            if (hasFuel()) {
                pausedBurns.add(currentBurn());
                clearBurn();
            }
            if (best.saved != null) {
                pausedBurns.remove(best.saved);
                restore(best.saved);
                parent.setChangedFast();
                return temperature;
            }
            if (best.source == null) {
                ItemStack stack = solidSource.get();
                int duration = solidTime();
                if (duration <= 0) return 0;
                ItemStack remainder = stack.copy();
                remainder.shrink(1);
                ItemStack container = stack.copyWithCount(1).getCraftingRemainingItem();
                // Container fuels occupy one slot, so their remainder can stay in that same slot.
                if (!container.isEmpty()) remainder = container;
                fuel = fuelQuality = duration;
                var recipe = MeltingFuelLookup.getSolid();
                temperature = recipe.getTemperature();
                rate = recipe.getRate();
                lastFuel = FluidStack.EMPTY;
                solidRemainder.accept(remainder);
                parent.setChangedFast();
                return temperature;
            }
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
        ListTag paused = new ListTag();
        pausedBurns.forEach(burn -> paused.add(burn.save()));
        tag.put("pausedBurns", paused);
        return tag;
    }
    @Override public void readFromTag(CompoundTag tag) {
        super.readFromTag(tag);
        fuelQuality = tag.getInt("quality");
        lastFuel = FluidStack.loadFluidStackFromNBT(tag.getCompound("lastFuel"));
        pausedBurns.clear();
        for (Tag entry : tag.getList("pausedBurns", Tag.TAG_COMPOUND)) {
            Burn burn = Burn.load((CompoundTag)entry);
            if (burn.remaining > 0) pausedBurns.add(burn);
        }
    }
}
