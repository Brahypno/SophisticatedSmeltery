package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeItem;
import org.brahypno.sophisticated_smeltery.casting.PortableCasting;
import org.brahypno.sophisticated_smeltery.furnace.UpgradeTankHandler;
import org.brahypno.sophisticated_smeltery.furnace.StorageSlotTransfer;

public final class CastingUpgradeWrapper extends UpgradeWrapperBase<CastingUpgradeWrapper, CastingUpgradeItem> implements ITickableUpgrade {
    public static final String STATE = "casting";
    private final PortableCasting casting;
    private boolean dirty;
    private boolean ticking;
    private boolean pourRequested = true;
    private List<FluidStack> lastFluids = List.of();
    private record FluidSource(IFluidHandler handler, Runnable save) {}

    public CastingUpgradeWrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) {
        super(storage, upgrade, save);
        CompoundTag state = upgrade.getTag() == null ? new CompoundTag() : upgrade.getTag().getCompound(STATE);
        casting = new PortableCasting(state, this::changed, upgradeItem.isScorched());
    }

    private void changed() {
        dirty = true;
        pourRequested = true;
        if (!ticking) persist();
    }

    public PortableCasting casting() { return casting; }
    @Override public boolean canBeDisabled() { return false; }
    public boolean isCastingEnabled() { return upgrade.getTag() == null || !upgrade.getTag().getBoolean("castingDisabled"); }
    public void setCastingEnabled(boolean enabled) {
        upgrade.getOrCreateTag().putBoolean("castingDisabled", !enabled);
        pourRequested = true;
        persist();
    }
    public boolean isOutputToStorage() { return upgrade.getTag() != null && upgrade.getTag().getBoolean("outputToStorage"); }
    public void setOutputToStorage(boolean storage) {
        upgrade.getOrCreateTag().putBoolean("outputToStorage", storage);
        pourRequested = true;
        persist();
    }
    public boolean supportsAutomatic() { return upgradeItem.isAdvanced(); }
    public boolean isAutomatic() { return supportsAutomatic() && upgrade.getTag() != null && upgrade.getTag().getBoolean("automatic"); }
    public void setAutomatic(boolean automatic) {
        if (!supportsAutomatic()) return;
        upgrade.getOrCreateTag().putBoolean("automatic", automatic);
        pourRequested = true;
        persist();
    }
    public Optional<MeltingUpgradeWrapper> controller() {
        return storageWrapper.getUpgradeHandler().getTypeWrappers(MeltingUpgradeItem.TYPE).stream().findFirst();
    }
    public boolean canRemove() { return !casting.hasFluid(); }

    private List<FluidSource> fluidSources() {
        List<FluidSource> sources = new ArrayList<>();
        controller().ifPresent(controller -> sources.add(new FluidSource(controller.getTank(), controller::persist)));
        storageWrapper.getUpgradeHandler().getTypeWrappers(AlloyerUpgradeItem.TYPE)
                .forEach(alloyer -> sources.add(new FluidSource(alloyer.getTank(), alloyer::persist)));
        storageWrapper.getUpgradeHandler().getTypeWrappers(TankUpgradeItem.TYPE)
                .forEach(tank -> sources.add(new FluidSource(new UpgradeTankHandler(tank), () -> {})));
        return sources;
    }

    public boolean hasFluidStorage() { return !fluidSources().isEmpty(); }
    public FluidStack previewFluid(Level level) {
        for (FluidSource source : fluidSources()) {
            FluidStack fluid = source.handler().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if (!casting.preview(level, fluid).isEmpty()) return fluid;
        }
        return FluidStack.EMPTY;
    }

    public int pour(Level level) {
        if (level.isClientSide || !isCastingEnabled()) return 0;
        refillCast();
        return pour(level, fluidSources());
    }

    private int pour(Level level, List<FluidSource> sources) {
        int amount = 0;
        for (FluidSource source : sources) {
            int poured = casting.pour(level, source.handler());
            if (poured > 0) { source.save().run(); amount += poured; }
            if (casting.isCooling()) break;
        }
        if (dirty) persist();
        return amount;
    }
    public int returnFluid(Level level) {
        if (level.isClientSide) return 0;
        int amount = 0;
        for (FluidSource source : fluidSources()) {
            int returned = casting.returnFluid(level, source.handler());
            if (returned > 0) { source.save().run(); amount += returned; }
            if (!casting.hasFluid()) break;
        }
        if (dirty) persist();
        return amount;
    }
    public boolean setMode(PortableCasting.Mode mode) {
        boolean changed = casting.setMode(mode);
        if (dirty) persist();
        return changed;
    }

    @Override public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (level.isClientSide) return;
        ticking = true;
        try {
            casting.bind(level);
            if (isCastingEnabled()) refillCast();
            if (isOutputToStorage()) exportOutput();
            if (isCastingEnabled() && isAutomatic() && !casting.isCooling()) {
                var sources = fluidSources();
                // Observe full contents: simulated drain can hide changes behind a tank's transfer limit.
                boolean fluidChanged = sources.size() != lastFluids.size();
                for (int i = 0; !fluidChanged && i < sources.size(); i++)
                    fluidChanged = !sources.get(i).handler().getFluidInTank(0).isFluidStackIdentical(lastFluids.get(i));
                if (fluidChanged) {
                    List<FluidStack> fluids = new ArrayList<>(sources.size());
                    for (FluidSource source : sources) fluids.add(source.handler().getFluidInTank(0).copy());
                    lastFluids = fluids;
                }
                if (pourRequested || fluidChanged) {
                    pour(level, sources);
                    pourRequested = false;
                }
            }
            // Cooling and casting intentionally do not depend on the controller heating switch.
            casting.tick(level);
            if (isOutputToStorage()) exportOutput();
        } finally { ticking = false; }
        if (dirty) persist();
    }

    private void refillCast() {
        if (casting.hasFluid() || !casting.inventory().getStackInSlot(0).isEmpty()) return;
        ItemStack target = casting.lockedCast();
        if (target.isEmpty()) return;
        ItemStack taken = StorageSlotTransfer.extractOne(storageWrapper.getInventoryHandler(), target);
        if (!taken.isEmpty()) casting.inventory().setStackInSlot(0, taken);
    }

    private void exportOutput() {
        if (casting.hasFluid()) return;
        if (casting.resultInInput()) exportSlot(0);
        exportSlot(1);
    }

    private void exportSlot(int outputSlot) {
        ItemStack original = casting.inventory().getStackInSlot(outputSlot);
        if (original.isEmpty()) return;
        int meltingSlots = controller().map(MeltingUpgradeWrapper::getMeltingSlotCount).orElse(0);
        ItemStack remaining = StorageSlotTransfer.insert(storageWrapper, original.copy(), meltingSlots);
        if (remaining.getCount() != original.getCount()) casting.setOutputRemainder(outputSlot, remaining);
    }

    public void persist() {
        upgrade.getOrCreateTag().put(STATE, casting.save());
        dirty = false;
        save();
    }
    @Override public void onBeforeRemoved() { persist(); }
}
