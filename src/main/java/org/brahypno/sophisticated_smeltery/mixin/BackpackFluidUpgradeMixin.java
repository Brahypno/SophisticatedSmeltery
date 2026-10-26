package org.brahypno.sophisticated_smeltery.mixin;

import net.p3pp3rf1y.sophisticatedbackpacks.api.IFluidHandlerWrapperUpgrade;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;

/** Add the optional backpack API only when that mod is installed. */
@Mixin(value = {MeltingUpgradeWrapper.class, AlloyerUpgradeWrapper.class}, remap = false)
public abstract class BackpackFluidUpgradeMixin implements IFluidHandlerWrapperUpgrade {}
