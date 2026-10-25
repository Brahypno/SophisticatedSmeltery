package org.brahypno.sophisticated_smeltery.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;

public final class AlloyerUpgradeContainer extends UpgradeContainerBase<AlloyerUpgradeWrapper, AlloyerUpgradeContainer> {
    public AlloyerUpgradeContainer(Player player, int id, AlloyerUpgradeWrapper wrapper,
                                  UpgradeContainerType<AlloyerUpgradeWrapper, AlloyerUpgradeContainer> type) {
        super(player, id, wrapper, type);
        for (int slot = 0; slot < 9; slot++) slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.getComponents(), slot, -2000, -2000));
        slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.getContainers(), 0, -2000, -2000));
        slots.add(new SlotSuppliedHandler(() -> upgradeWrapper.getContainers(), 1, -2000, -2000) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
    }
    public void action(String action) {
        sendDataToServer(() -> { CompoundTag data = new CompoundTag(); data.putString("action", action); return data; });
    }
    /** 0 is the built-in tank, 1..8 are ingredient tanks, and 9 is liquid fuel. */
    public void fluidClick(int target, boolean emptyItem) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putString("action", "fluid");
            data.putInt("target", target);
            data.putBoolean("emptyItem", emptyItem);
            return data;
        });
    }
    public void transfer(int index, boolean intoAlloyer) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putString("action", "transfer");
            data.putInt("index", index);
            data.putBoolean("intoAlloyer", intoAlloyer);
            return data;
        });
    }
    @Override public void handleMessage(CompoundTag data) {
        switch (data.getString("action")) {
            case "heating" -> upgradeWrapper.setHeating(!upgradeWrapper.isHeating());
            case "bucketMode" -> upgradeWrapper.cycleTransferDirection();
            case "bucketTarget" -> upgradeWrapper.cycleContainerTarget();
            case "transfer" -> upgradeWrapper.transferBackpackTank(data.getInt("index"), data.getBoolean("intoAlloyer"));
            case "fluid" -> {
                int target = data.getInt("target");
                if (target < 0 || target > 9) return;
                IFluidHandler tank = target == 0 ? upgradeWrapper.getTank()
                        : target == 9 ? upgradeWrapper.alloyer().fuel() : upgradeWrapper.getInputTank(target - 1);
                ItemStack carried = player.containerMenu.getCarried();
                if (!carried.isEmpty()) {
                    var result = FluidTransferHelper.interactWithStack(tank, carried,
                            data.getBoolean("emptyItem") ? TransferDirection.EMPTY_ITEM : TransferDirection.FILL_ITEM);
                    player.containerMenu.setCarried(FluidTransferHelper.handleUIResult(player, carried, result));
                    upgradeWrapper.persist();
                }
            }
            default -> { }
        }
    }
}
