package org.brahypno.sophisticated_smeltery.furnace;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Storage operations shared by melting controllers and the independent alloyer upgrade. */
public interface FluidStorageUpgrade {
    IFluidHandler getTank();
    List<IFluidHandler> getInternalFuelTanks();
    void persist();
    IStorageFluidHandler wrapHandler(@Nullable IStorageFluidHandler next, ItemStack container);
}
