package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;

public final class MeltingUpgradeItem extends UpgradeItemBase<MeltingUpgradeWrapper> {
    public static final UpgradeType<MeltingUpgradeWrapper> TYPE = new UpgradeType<>(MeltingUpgradeWrapper::new);
    private static final IUpgradeCountLimitConfig SINGLE_CONTROLLER = new IUpgradeCountLimitConfig() {
        public int getMaxUpgradesPerStorage(String storage, ResourceLocation item) { return 1; }
        public int getMaxUpgradesInGroupPerStorage(String storage, UpgradeGroup group) { return Integer.MAX_VALUE; }
    };

    public MeltingUpgradeItem() { super(SINGLE_CONTROLLER); }
    @Override public UpgradeType<MeltingUpgradeWrapper> getType() { return TYPE; }
    @Override public int getMaxStackSize(ItemStack stack) { return stack.hasTag() && stack.getTag().contains(ControllerBinding.CONTROLLER) ? 1 : 64; }

    @Override public Component getName(ItemStack stack) {
        ItemStack controller = ControllerBinding.getController(stack);
        if (!ControllerBinding.supports(controller)) return super.getName(stack);
        return Component.translatable("item.sophisticated_smeltery.melting_upgrade.bound", ControllerBinding.getControllerName(controller));
    }

    @Override public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        return List.of(new IUpgradeItem.UpgradeConflictDefinition(item -> item instanceof MeltingUpgradeItem, 0,
                Component.translatable("error.sophisticated_smeltery.one_controller")));
    }

    @Override public UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack stack, IStorageWrapper storage,
                                                                       boolean client, IUpgradeItem<?> replacing) {
        return ControllerBinding.supports(ControllerBinding.getController(stack)) ? new UpgradeSlotChangeResult.Success()
                : fail("bind_controller");
    }

    @Override public UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storage, boolean client) {
        for (var wrapper : storage.getUpgradeHandler().getSlotWrappers().values()) {
            if (wrapper instanceof MeltingUpgradeWrapper melting && !melting.canRemove()) return fail("empty_fluids");
        }
        return new UpgradeSlotChangeResult.Success();
    }

    @Override public UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack target, int upgradeSlot, IStorageWrapper storage, boolean client) {
        UpgradeSlotChangeResult remove = canRemoveUpgradeFrom(storage, client);
        if (!remove.isSuccessful()) return remove;
        if (target.getItem() == this) return checkExtraInsertConditions(target, storage, client, this);
        return super.canSwapUpgradeFor(target, upgradeSlot, storage, client);
    }

    private static UpgradeSlotChangeResult fail(String key) {
        return new UpgradeSlotChangeResult.Fail(Component.translatable("error.sophisticated_smeltery." + key), Set.of(), Set.of(), Set.of());
    }
}
