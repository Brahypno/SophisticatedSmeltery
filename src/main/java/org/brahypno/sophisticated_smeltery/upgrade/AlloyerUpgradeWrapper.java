package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IFluidHandlerWrapperUpgrade;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeItem;
import org.brahypno.sophisticated_smeltery.furnace.FluidStorageUpgrade;
import org.brahypno.sophisticated_smeltery.furnace.FurnaceFluidHandler;
import org.brahypno.sophisticated_smeltery.furnace.PortableAlloyer;
import org.brahypno.sophisticated_smeltery.furnace.UpgradeTankHandler;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

public final class AlloyerUpgradeWrapper extends UpgradeWrapperBase<AlloyerUpgradeWrapper, AlloyerUpgradeItem>
        implements ITickableUpgrade, IStackableContentsUpgrade, IFluidHandlerWrapperUpgrade, FluidStorageUpgrade {
    public static final String STATE = "alloyer";
    private final PortableAlloyer alloyer;
    private final ItemStackHandler components;
    private final ItemStackHandler containers;
    private boolean loading = true;
    private boolean transferring;
    private boolean dirty;
    private int lastFluidRevision = -1;

    public AlloyerUpgradeWrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) {
        super(storage, upgrade, save);
        CompoundTag state = upgrade.hasTag() ? upgrade.getTag().getCompound(STATE) : new CompoundTag();
        alloyer = new PortableAlloyer(TinkerSmeltery.alloyer.get().create(BlockPos.ZERO,
                TinkerSmeltery.scorchedAlloyer.get().defaultBlockState()), state, this::fuelSources, () -> dirty = true);
        components = new ItemStackHandler(9) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return slot == PortableAlloyer.FUEL_SLOT ? PortableAlloyer.acceptsFuel(stack) : PortableAlloyer.acceptsInput(stack);
            }
            @Override public int getSlotLimit(int slot) { return slot < PortableAlloyer.INPUTS ? 1 : 64; }
            @Override protected int getStackLimit(int slot, ItemStack stack) {
                return slot < PortableAlloyer.INPUTS || PortableAlloyer.isFuelTank(stack) || stack.hasCraftingRemainingItem()
                        ? 1 : super.getStackLimit(slot, stack);
            }
            @Override protected void onContentsChanged(int slot) { if (!loading) { transferContainer(); persist(); } }
        };
        containers = new ItemStackHandler(2) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && (FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack)
                        || stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent());
            }
            @Override protected void onContentsChanged(int slot) { if (!loading && !transferring) { transferContainer(); persist(); } }
        };
        if (state.contains("components")) components.deserializeNBT(state.getCompound("components"));
        if (state.contains("containers")) containers.deserializeNBT(state.getCompound("containers"));
        alloyer.setComponents(components);
        updateCapacity();
        loading = false;
    }

    public PortableAlloyer alloyer() { return alloyer; }
    @Override public FluidTankAnimated getTank() { return alloyer.tank(); }
    public ItemStackHandler getComponents() { return components; }
    public ItemStackHandler getContainers() { return containers; }
    @Override public boolean canBeDisabled() { return false; }
    public boolean isHeating() { return !upgrade.hasTag() || !upgrade.getTag().getBoolean("alloyerDisabled"); }
    public void setHeating(boolean enabled) { upgrade.getOrCreateTag().putBoolean("alloyerDisabled", !enabled); persist(); }
    public int getContainerTarget() { return upgrade.hasTag() ? Math.max(0, Math.min(8, upgrade.getTag().getInt("bucketTarget"))) : 0; }
    public void cycleContainerTarget() {
        upgrade.getOrCreateTag().putInt("bucketTarget", (getContainerTarget() + 1) % 9);
        transferContainer();
        persist();
    }
    public IFluidHandler getInputTank(int slot) {
        IFluidHandler handler = alloyer.input(slot);
        return handler == null ? EmptyFluidHandler.INSTANCE : handler;
    }
    public IFluidHandler getContainerTank() { return getContainerTarget() == 0 ? getTank() : getInputTank(getContainerTarget() - 1); }
    public TransferDirection getTransferDirection() {
        int direction = upgrade.hasTag() ? upgrade.getTag().getInt("bucketMode") : 0;
        return switch (direction) { case 1 -> TransferDirection.EMPTY_ITEM; case 2 -> TransferDirection.FILL_ITEM; default -> TransferDirection.AUTO; };
    }
    public void cycleTransferDirection() {
        upgrade.getOrCreateTag().putInt("bucketMode", (upgrade.getOrCreateTag().getInt("bucketMode") + 1) % 3);
        transferContainer();
        persist();
    }
    public void transferContainer() {
        if (loading || transferring || !net.minecraftforge.fml.util.thread.EffectiveSide.get().isServer()
                || containers.getStackInSlot(0).isEmpty() || !containers.getStackInSlot(1).isEmpty()) return;
        transferring = true;
        try {
            ItemStack input = containers.getStackInSlot(0).copy();
            var result = FluidTransferHelper.interactWithStack(getContainerTank(), input, getTransferDirection());
            if (result != null) {
                containers.setStackInSlot(0, input);
                containers.setStackInSlot(1, result.stack());
            }
        } finally { transferring = false; }
    }
    public List<IFluidHandler> getBackpackTanks() {
        List<IFluidHandler> tanks = new ArrayList<>();
        storageWrapper.getUpgradeHandler().getTypeWrappers(TankUpgradeItem.TYPE).forEach(tank -> tanks.add(new UpgradeTankHandler(tank)));
        return tanks;
    }
    @Override public List<IFluidHandler> getInternalFuelTanks() {
        ItemStack stack = components.getStackInSlot(PortableAlloyer.FUEL_SLOT);
        IFluidHandler tank = PortableAlloyer.isFuelTank(stack) ? stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null) : null;
        return tank == null ? List.of() : List.of(tank);
    }
    private List<IFluidHandler> fuelSources() {
        List<IFluidHandler> tanks = new ArrayList<>(getInternalFuelTanks());
        tanks.addAll(getBackpackTanks());
        return tanks;
    }
    public void transferBackpackTank(int index, boolean intoAlloyer) {
        var tanks = getBackpackTanks();
        if (index < 0 || index >= tanks.size()) return;
        IFluidHandler source = intoAlloyer ? tanks.get(index) : getTank();
        IFluidHandler target = intoAlloyer ? getTank() : tanks.get(index);
        FluidStack possible = source.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        int amount = target.fill(possible, IFluidHandler.FluidAction.SIMULATE);
        if (amount <= 0) return;
        FluidStack drained = source.drain(new FluidStack(possible, amount), IFluidHandler.FluidAction.EXECUTE);
        int filled = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled < drained.getAmount()) source.fill(new FluidStack(drained, drained.getAmount() - filled), IFluidHandler.FluidAction.EXECUTE);
        persist();
    }
    private void updateCapacity() { alloyer.resize(ModItems.TANK_UPGRADE.get().getAdjustedStackMultiplier(storageWrapper)); }
    public boolean canRemove() {
        if (!getTank().isEmpty()) return false;
        for (int slot = 0; slot < components.getSlots(); slot++) {
            IFluidHandler tank = components.getStackInSlot(slot).getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            if (tank != null) for (int index = 0; index < tank.getTanks(); index++) if (!tank.getFluidInTank(index).isEmpty()) return false;
        }
        return true;
    }
    @Override public int getMinimumMultiplierRequired() {
        if (getTank().getFluidAmount() <= alloyer.baseCapacity()) return 1;
        double ratio = ModItems.TANK_UPGRADE.get().getTankUpgradeConfig().stackMultiplierRatio.get();
        return ratio == 0 ? Integer.MAX_VALUE : (int)Math.min(Integer.MAX_VALUE,
                Math.ceil(1 + ((double)getTank().getFluidAmount() / alloyer.baseCapacity() - 1) / ratio));
    }
    @Override public IStorageFluidHandler wrapHandler(@Nullable IStorageFluidHandler next, ItemStack backpack) {
        return new FurnaceFluidHandler(this, next, backpack);
    }
    @Override public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (level.isClientSide) return;
        alloyer.bind(level);
        updateCapacity();
        alloyer.tick(isHeating(), level.getGameTime());
        if (lastFluidRevision != alloyer.fluidRevision()) {
            transferContainer();
            lastFluidRevision = alloyer.fluidRevision();
        }
        if (dirty) persist();
    }
    @Override public void persist() {
        if (loading) return;
        CompoundTag state = alloyer.save();
        state.put("components", components.serializeNBT());
        state.put("containers", containers.serializeNBT());
        upgrade.getOrCreateTag().put(STATE, state);
        dirty = false;
        save();
    }
    @Override public void onBeforeRemoved() { persist(); }
    public static boolean cleanForClearing(ItemStack upgrade) {
        return !upgrade.hasTag() || FluidStack.loadFluidStackFromNBT(upgrade.getTag().getCompound(STATE).getCompound("tank")).isEmpty();
    }
    public static List<ItemStack> storedItems(ItemStack upgrade) {
        List<ItemStack> items = new ArrayList<>();
        if (!upgrade.hasTag()) return items;
        CompoundTag state = upgrade.getTag().getCompound(STATE);
        for (String key : List.of("components", "containers")) {
            for (Tag entry : state.getCompound(key).getList("Items", Tag.TAG_COMPOUND)) {
                ItemStack stack = ItemStack.of((CompoundTag)entry);
                if (!stack.isEmpty()) items.add(stack);
            }
        }
        return items;
    }
}
