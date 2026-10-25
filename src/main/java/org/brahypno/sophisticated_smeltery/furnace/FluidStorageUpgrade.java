package org.brahypno.sophisticated_smeltery.furnace;

import java.util.List;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Storage operations shared by melting controllers and the independent alloyer upgrade. */
public interface FluidStorageUpgrade {
    IFluidHandler getTank();
    List<IFluidHandler> getInternalFuelTanks();
    void persist();
}
