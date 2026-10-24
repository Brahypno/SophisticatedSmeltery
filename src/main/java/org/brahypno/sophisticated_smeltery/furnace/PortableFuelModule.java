package org.brahypno.sophisticated_smeltery.furnace;

import java.util.List;
import java.util.function.Supplier;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.material.Fluid;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;

/** Reuses native fuel recipes and burn accounting without looking up world tanks. */
public final class PortableFuelModule extends FuelModule {
    private final Supplier<List<IFluidHandler>> sources;

    public PortableFuelModule(MantleBlockEntity parent, Supplier<List<IFluidHandler>> sources) {
        super(parent);
        this.sources = sources;
    }

    // The native shared lookup is cached and invalidated on datapack reload.
    // Avoid retaining an obsolete recipe in the detached fuel module.
    @Override protected MeltingFuel findRecipe(Fluid fluid) {
        return MeltingFuelLookup.findFuel(fluid);
    }

    @Override public int findFuel(boolean consume) {
        List<IFluidHandler> available = sources.get();
        for (IFluidHandler source : available) {
            int temperature = tryLiquidFuel(source, consume);
            if (temperature > 0) return temperature;
        }
        return 0;
    }
}
