package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.brahypno.sophisticated_smeltery.compat.StorageCompat;
import org.brahypno.sophisticated_smeltery.furnace.MobMelting;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeItem;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.mixin.RenderInfoAccess;
import org.brahypno.sophisticated_smeltery.furnace.NativeFurnace;
import org.brahypno.sophisticated_smeltery.furnace.PortableMobMelting;
import org.brahypno.sophisticated_smeltery.furnace.UpgradeTankHandler;
import org.brahypno.sophisticated_smeltery.furnace.FurnaceFluidHandler;
import org.brahypno.sophisticated_smeltery.furnace.FluidStorageUpgrade;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;

public final class MeltingUpgradeWrapper extends UpgradeWrapperBase<MeltingUpgradeWrapper, MeltingUpgradeItem>
        implements ITickableUpgrade, IStackableContentsUpgrade, FluidStorageUpgrade {
    @Nullable private final NativeFurnace furnace;
    // Inactive upgrades expose no fluid capacity and never notify a tank parent.
    private final SmelteryTank<?> emptyTank = new SmelteryTank<>(null);
    @Nullable private final MobMelting mobMelting;
    private final ItemStackHandler components;
    private final ItemStackHandler containers;
    private boolean transferring;
    private boolean loading = true;
    private boolean dirty;
    private boolean renderedActive;
    private int lastMaterials = -1;
    private double lastCapacityMultiplier = -1;
    private int[] inputSlots = new int[0];
    private int lastContainerFluidRevision = -1;

    public MeltingUpgradeWrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) {
        super(storage, upgrade, save);
        renderedActive = ControllerBinding.isActive(upgrade);
        CompoundTag tag = upgrade.getTag();
        CompoundTag state = tag == null ? new CompoundTag() : tag.getCompound(ControllerBinding.STATE);
        var controller = ControllerBinding.create(ControllerBinding.getController(upgrade));
        furnace = controller == null ? null : new NativeFurnace(controller, state, this::fuelSources, () -> dirty = true);
        mobMelting = ModList.get().isLoaded("sophisticatedbackpacks") ? new PortableMobMelting(this, state) : null;
        components = new ItemStackHandler(9) {
            @Override public boolean isItemValid(int slot, ItemStack stack) { return furnace != null && furnace.acceptsMaterial(stack); }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ItemStack result = super.extractItem(slot, amount, true);
                if (!result.isEmpty() && !fitsAfterRemoving(result.getCount())) return ItemStack.EMPTY;
                return super.extractItem(slot, amount, simulate);
            }
            @Override protected void onContentsChanged(int slot) {
                if (!loading && furnace != null) {
                    updateCapacity();
                    furnace.updateHeatComponents(components);
                    persist();
                }
            }
        };
        containers = new ItemStackHandler(2) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return isOperational() && slot == 0 && (FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack)
                        || stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent());
            }
            @Override protected void onContentsChanged(int slot) {
                if (!loading && !transferring) { transferContainer(); persist(); }
            }
        };
        // Keep unknown item/fluid IDs in their original NBT while the controller is absent.
        if (furnace != null) {
            if (state.contains("components")) components.deserializeNBT(state.getCompound("components"));
            if (state.contains("containers")) containers.deserializeNBT(state.getCompound("containers"));
        }
        loading = false;
        if (furnace != null) furnace.updateHeatComponents(components);
        applyTransmuteControls();
    }

    public ItemStackHandler getComponents() { return components; }
    public ItemStackHandler getContainers() { return containers; }
    public boolean isOperational() { return furnace != null; }
    public boolean hasTransmuteControls() { return furnace != null && furnace.isTransmute() && ((MeltingUpgradeItem)upgrade.getItem()).isAdvanced(); }
    public int getTransmuteMode() { return hasTransmuteControls() && upgrade.hasTag() ? Math.floorMod(upgrade.getTag().getInt("transmuteMode"), 3) : 0; }
    public boolean isTransmuteAlloying() { return hasTransmuteControls() && upgrade.hasTag() && upgrade.getTag().getBoolean("transmuteAlloying"); }
    private void applyTransmuteControls() {
        if (furnace != null) furnace.setTransmuteControls(getTransmuteMode(), isTransmuteAlloying());
    }
    public void cycleTransmuteMode() {
        if (!hasTransmuteControls()) return;
        upgrade.getOrCreateTag().putInt("transmuteMode", (getTransmuteMode() + 1) % 3);
        applyTransmuteControls();
        persist();
    }
    public void toggleTransmuteAlloying() {
        if (!hasTransmuteControls()) return;
        upgrade.getOrCreateTag().putBoolean("transmuteAlloying", !isTransmuteAlloying());
        applyTransmuteControls();
        persist();
    }
    @Nullable public NativeFurnace getFurnace() { return furnace; }
    public IStorageWrapper getStorage() { return storageWrapper; }
    public SmelteryTank<?> getTank() { return furnace == null ? emptyTank : furnace.controller().getTank(); }
    @Override public IStorageFluidHandler wrapHandler(@Nullable IStorageFluidHandler handler, ItemStack backpack) {
        return new FurnaceFluidHandler(this, handler, backpack);
    }
    // Keep the wrapper ticking so the explicit heating switch can still cool items.
    @Override public boolean canBeDisabled() { return false; }
    public boolean isHeating() { return isOperational() && (upgrade.getTag() == null || !upgrade.getTag().contains("heating") || upgrade.getTag().getBoolean("heating")); }

    public void setHeating(boolean value) { if (!isOperational()) return; upgrade.getOrCreateTag().putBoolean("heating", value); persist(); }
    public TransferDirection getTransferDirection() {
        int mode = upgrade.getOrCreateTag().getInt("bucketMode");
        return switch (mode) { case 1 -> TransferDirection.EMPTY_ITEM; case 2 -> TransferDirection.FILL_ITEM; default -> TransferDirection.AUTO; };
    }
    public void cycleTransferDirection() {
        if (!isOperational()) return;
        upgrade.getOrCreateTag().putInt("bucketMode", (upgrade.getOrCreateTag().getInt("bucketMode") + 1) % 3);
        transferContainer();
        persist();
    }
    public void transferContainer() {
        if (!isOperational() || !net.minecraftforge.fml.util.thread.EffectiveSide.get().isServer() || loading || transferring || containers.getStackInSlot(0).isEmpty() || !containers.getStackInSlot(1).isEmpty()) return;
        transferring = true;
        try {
            ItemStack input = containers.getStackInSlot(0).copy();
            var result = FluidTransferHelper.interactWithStack(getTank(), input, getTransferDirection());
            if (result != null) {
                containers.setStackInSlot(0, input);
                containers.setStackInSlot(1, result.stack());
            }
        } finally { transferring = false; }
    }

    private int materialCount() {
        int count = 1; // The bound controller is one of the structure blocks.
        for (int slot = 0; slot < components.getSlots(); slot++) {
            ItemStack stack = components.getStackInSlot(slot);
            if (furnace.acceptsMaterial(stack)) count += stack.getCount();
        }
        return count;
    }

    private double capacityMultiplier() { return StorageCompat.tankMultiplier(storageWrapper); }

    private void updateCapacity() {
        int count = materialCount();
        double multiplier = capacityMultiplier();
        if (count != lastMaterials || multiplier != lastCapacityMultiplier) {
            furnace.resize(count, multiplier);
            lastMaterials = count;
            lastCapacityMultiplier = multiplier;
            furnace.updateHeatComponents(components);
        }
    }

    private boolean fitsAfterRemoving(int count) {
        if (furnace == null) return true;
        int capacity = (int)Math.min(Integer.MAX_VALUE, furnace.calculate(materialCount() - count).capacity() * capacityMultiplier());
        return getTank().getContained() <= capacity;
    }

    public int getMeltingSlotCount() {
        if (furnace == null) return 0;
        long slots = (long)Math.min(Integer.MAX_VALUE, furnace.dimensions().volume() * storageWrapper.getInventoryHandler().getStackSizeMultiplier());
        return (int)Math.min(slots, storageWrapper.getInventoryHandler().getSlots());
    }

    public boolean isMeltingSlot(int slot) {
        int total = storageWrapper.getInventoryHandler().getSlots();
        int count = getMeltingSlotCount();
        return slot >= 0 && slot < total && slot < count;
    }

    @Override public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (level.isClientSide || furnace == null) return;
        if (furnace.refreshRecipes()) lastMaterials = -1;
        updateCapacity();
        int count = getMeltingSlotCount();
        if (inputSlots.length != count) {
            inputSlots = new int[count];
            for (int index = 0; index < count; index++) inputSlots[index] = index;
        }
        furnace.bindInputs(level, storageWrapper.getInventoryHandler(), inputSlots);
        if ((level.getGameTime() & 3) == 0 || (level.getGameTime() & 3) == 1) furnace.refreshInputs();
        furnace.tick(isHeating() && furnace.dimensions().volume() > 0, level.getGameTime());
        if (mobMelting != null && level instanceof net.minecraft.server.level.ServerLevel server) mobMelting.tick(entity, server, pos);
        // Retry a waiting container only when fluid contents/order actually change.
        // Recipe progress and fuel burning must not cause repeated container lookups.
        if (lastContainerFluidRevision != furnace.fluidRevision()) {
            transferContainer();
            lastContainerFluidRevision = furnace.fluidRevision();
        }
        if (dirty) persist();
    }

    public List<IFluidHandler> getBackpackTanks() {
        List<IFluidHandler> result = new ArrayList<>();
        storageWrapper.getUpgradeHandler().getTypeWrappers(TankUpgradeItem.TYPE)
                .forEach(tank -> result.add(new UpgradeTankHandler(tank)));
        return result;
    }

    public List<IFluidHandler> getInternalFuelTanks() {
        List<IFluidHandler> result = new ArrayList<>();
        if (furnace == null) return result;
        for (int slot = 0; slot < components.getSlots(); slot++) {
            ItemStack stack = components.getStackInSlot(slot);
            if (furnace.isFuelTank(stack)) stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(result::add);
        }
        return result;
    }

    private List<IFluidHandler> fuelSources() {
        List<IFluidHandler> result = getInternalFuelTanks();
        result.addAll(getBackpackTanks());
        return result;
    }

    public boolean canRemove() {
        if (furnace == null) return true;
        if (getTank().getContained() > 0) return false;
        for (int slot = 0; slot < components.getSlots(); slot++) {
            IFluidHandler handler = components.getStackInSlot(slot).getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            if (handler != null) {
                for (int tank = 0; tank < handler.getTanks(); tank++) if (!handler.getFluidInTank(tank).isEmpty()) return false;
            }
        }
        return true;
    }

    @Override public int getMinimumMultiplierRequired() {
        if (furnace == null) return 1;
        int volume = getTank().getContained();
        int base = furnace.baseCapacity();
        if (volume <= base) return 1;
        double ratio = StorageCompat.tankRatio(storageWrapper);
        if (base == 0 || ratio == 0) return Integer.MAX_VALUE;
        return (int)Math.min(Integer.MAX_VALUE, Math.ceil(1 + ((double)volume / base - 1) / ratio));
    }

    public void moveFluidToBottom(int index) {
        if (index >= 0 && index < getTank().getFluids().size()) {
            getTank().moveFluidToBottom(index);
            persist();
        }
    }

    public void transferBackpackTank(int index, boolean intoFurnace) {
        List<IFluidHandler> tanks = getBackpackTanks();
        if (index < 0 || index >= tanks.size()) return;
        IFluidHandler source = intoFurnace ? tanks.get(index) : getTank();
        IFluidHandler target = intoFurnace ? getTank() : tanks.get(index);
        FluidStack possible = source.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        int accepted = target.fill(possible, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;
        FluidStack drained = source.drain(new FluidStack(possible, accepted), IFluidHandler.FluidAction.EXECUTE);
        int filled = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled < drained.getAmount()) source.fill(new FluidStack(drained, drained.getAmount() - filled), IFluidHandler.FluidAction.EXECUTE);
        persist();
    }

    public void persist() {
        if (loading || furnace == null) return;
        CompoundTag state = furnace.save();
        if (mobMelting != null) mobMelting.save(state);
        state.put("components", components.serializeNBT());
        state.put("containers", containers.serializeNBT());
        upgrade.getOrCreateTag().put(ControllerBinding.STATE, state);
        dirty = false;
        save();
        boolean active = ControllerBinding.isActive(upgrade);
        if (active != renderedActive) {
            renderedActive = active;
            // Upgrade render stacks are copies; fuel NBT changes alone do not
            // refresh the item/equipment snapshot or rebuild placed geometry.
            storageWrapper.getUpgradeHandler().setRenderUpgradeItems();
            ((RenderInfoAccess)storageWrapper.getRenderInfo()).sophisticatedsmeltery$save(true);
        }
    }

    @Override public void onBeforeRemoved() { persist(); }
}
