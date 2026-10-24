package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import org.brahypno.sophisticated_smeltery.casting.PortableCasting;

public final class CastingUpgradeWrapper extends UpgradeWrapperBase<CastingUpgradeWrapper, CastingUpgradeItem> implements ITickableUpgrade {
    public static final String STATE = "casting";
    private final PortableCasting casting;
    private boolean dirty;
    private boolean ticking;
    private boolean pourRequested = true;
    private MeltingUpgradeWrapper lastController;
    private int lastFluidRevision = -1;

    public CastingUpgradeWrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) {
        super(storage, upgrade, save);
        CompoundTag state = upgrade.getTag() == null ? new CompoundTag() : upgrade.getTag().getCompound(STATE);
        casting = new PortableCasting(state, this::changed);
    }

    private void changed() {
        dirty = true;
        pourRequested = true;
        if (!ticking) persist();
    }

    public PortableCasting casting() { return casting; }
    @Override public boolean canBeDisabled() { return false; }
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

    public int pour(Level level) {
        if (level.isClientSide) return 0;
        int amount = controller().map(controller -> {
            int poured = casting.pour(level, controller.getTank());
            if (poured > 0) controller.persist();
            return poured;
        }).orElse(0);
        if (dirty) persist();
        return amount;
    }
    public int returnFluid(Level level) {
        if (level.isClientSide) return 0;
        int amount = controller().map(controller -> {
            int returned = casting.returnFluid(level, controller.getTank());
            if (returned > 0) controller.persist();
            return returned;
        }).orElse(0);
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
            MeltingUpgradeWrapper controller = controller().orElse(null);
            if (controller != lastController) { pourRequested = true; lastController = controller; }
            if (controller != null && isAutomatic() && !casting.isCooling()
                    && casting.inventory().getStackInSlot(1).isEmpty()
                    && (pourRequested || lastFluidRevision != controller.getFurnace().fluidRevision())) {
                if (casting.pour(level, controller.getTank()) > 0) controller.persist();
                lastFluidRevision = controller.getFurnace().fluidRevision();
                pourRequested = false;
            }
            // Cooling and casting intentionally do not depend on the controller heating switch.
            casting.tick(level);
        } finally { ticking = false; }
        if (dirty) persist();
    }

    public void persist() {
        upgrade.getOrCreateTag().put(STATE, casting.save());
        dirty = false;
        save();
    }
    @Override public void onBeforeRemoved() { persist(); }
}
