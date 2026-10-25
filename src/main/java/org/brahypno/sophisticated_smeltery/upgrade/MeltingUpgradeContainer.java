package org.brahypno.sophisticated_smeltery.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

public final class MeltingUpgradeContainer extends UpgradeContainerBase<MeltingUpgradeWrapper, MeltingUpgradeContainer> {
    public MeltingUpgradeContainer(Player player, int id, MeltingUpgradeWrapper wrapper,
                                   UpgradeContainerType<MeltingUpgradeWrapper, MeltingUpgradeContainer> type) {
        super(player, id, wrapper, type);
        for (int slot = 0; slot < wrapper.getComponents().getSlots(); slot++) {
            slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.getComponents(), slot, -2000, -2000));
        }
        slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.getContainers(), 0, -2000, -2000));
        slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.getContainers(), 1, -2000, -2000) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
    }

    public void action(String action) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putString("action", action);
            return data;
        });
    }

    public void fluidClick(int index, boolean drain) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putString("action", "fluid");
            data.putInt("index", index);
            if (index >= 0 && index < upgradeWrapper.getTank().getFluids().size()) {
                data.put("fluid", upgradeWrapper.getTank().getFluids().get(index).writeToNBT(new CompoundTag()));
            }
            data.putBoolean("drain", drain);
            return data;
        });
    }

    public void transfer(int index, boolean intoFurnace) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putString("action", "transfer");
            data.putInt("index", index);
            data.putBoolean("intoFurnace", intoFurnace);
            return data;
        });
    }

    public void fuelClick(boolean emptyItem) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putString("action", "fuel");
            data.putBoolean("drain", emptyItem);
            return data;
        });
    }

    @Override public void handleMessage(CompoundTag data) {
        if (!upgradeWrapper.isOperational()) return;
        switch (data.getString("action")) {
            case "heating" -> upgradeWrapper.setHeating(!upgradeWrapper.isHeating());
            case "bucketMode" -> upgradeWrapper.cycleTransferDirection();
            case "transfer" -> upgradeWrapper.transferBackpackTank(data.getInt("index"), data.getBoolean("intoFurnace"));
            case "fluid" -> {
                int index = -1;
                var requested = net.minecraftforge.fluids.FluidStack.loadFluidStackFromNBT(data.getCompound("fluid"));
                for (int tank = 0; tank < upgradeWrapper.getTank().getFluids().size(); tank++) {
                    if (upgradeWrapper.getTank().getFluids().get(tank).isFluidEqual(requested)) { index = tank; break; }
                }
                ItemStack carried = player.containerMenu.getCarried();
                if (!carried.isEmpty()) {
                    var result = data.getBoolean("drain") || index < 0
                            ? FluidTransferHelper.interactWithStack(upgradeWrapper.getTank(), carried, TransferDirection.EMPTY_ITEM)
                            : FluidTransferHelper.fillStack(upgradeWrapper.getTank(), carried, requested);
                    player.containerMenu.setCarried(FluidTransferHelper.handleUIResult(player, carried, result));
                    upgradeWrapper.persist();
                } else if (index >= 0 && !data.getBoolean("drain")) {
                    upgradeWrapper.moveFluidToBottom(index);
                    upgradeWrapper.transferContainer();
                }
            }
            case "fuel" -> {
                ItemStack carried = player.containerMenu.getCarried();
                if (!carried.isEmpty()) {
                    var fuel = upgradeWrapper.getFurnace().fuel();
                    var result = data.getBoolean("drain")
                            ? FluidTransferHelper.interactWithStack(fuel, carried, TransferDirection.EMPTY_ITEM)
                            : FluidTransferHelper.fillStack(fuel, carried, fuel.getLastFluid());
                    player.containerMenu.setCarried(FluidTransferHelper.handleUIResult(player, carried, result));
                    upgradeWrapper.persist();
                }
            }
            default -> { }
        }
    }
}
