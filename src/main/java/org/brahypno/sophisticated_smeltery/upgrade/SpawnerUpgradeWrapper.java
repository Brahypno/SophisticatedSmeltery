package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import org.brahypno.sophisticated_smeltery.spawner.SpawnerRuntime;

/** Tick the registered native tile without placing a block or replacing world contents. */
public final class SpawnerUpgradeWrapper extends UpgradeWrapperBase<SpawnerUpgradeWrapper, SpawnerUpgradeItem> implements ITickableUpgrade {
    private final ItemStackHandler inventory;
    private SpawnerBlockEntity spawner;
    private boolean loading = true;
    private boolean ticking;

    public SpawnerUpgradeWrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) {
        super(storage, upgrade, save);
        inventory = new ItemStackHandler(1) {
            @Override public boolean isItemValid(int slot, ItemStack stack) { return stack.is(Items.SPAWNER); }
            @Override public int getSlotLimit(int slot) { return 1; }
            @Override protected void onContentsChanged(int slot) {
                if (loading || ticking) return;
                spawner = null;
                persist();
            }
        };
        if (upgrade.hasTag() && upgrade.getTag().contains("spawnerInventory", net.minecraft.nbt.Tag.TAG_COMPOUND))
            inventory.deserializeNBT(upgrade.getTag().getCompound("spawnerInventory"));
        loading = false;
    }
    public ItemStackHandler inventory() { return inventory; }
    public boolean isAdvanced() { return upgradeItem.isAdvanced(); }
    public SpawnerBlockEntity nativeSpawner() { return spawner; }
    @Override public boolean canBeDisabled() { return true; }

    @Override public void tick(@Nullable Entity host, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !isEnabled() || inventory.getStackInSlot(0).isEmpty()) return;
        if (spawner == null || spawner.getLevel() != level || !spawner.getBlockPos().equals(pos)) {
            spawner = BlockEntityType.MOB_SPAWNER.create(pos, Blocks.SPAWNER.defaultBlockState());
            if (spawner == null) return;
            // Apotheosis replaces this factory; its own tile and logic are retained.
            spawner.setLevel(level);
            CompoundTag state = BlockItem.getBlockEntityData(inventory.getStackInSlot(0));
            if (state != null) spawner.load(state.copy());
        }
        ticking = true;
        try {
            SpawnerRuntime.tick(server, pos, spawner.getSpawner(), isAdvanced(),
                    storageWrapper instanceof IBackpackWrapper backpack ? backpack : null);
            ItemStack stack = inventory.getStackInSlot(0);
            CompoundTag old = BlockItem.getBlockEntityData(stack);
            CompoundTag state = old == null ? new CompoundTag() : old.copy();
            state.merge(spawner.saveWithoutMetadata());
            // Never bake advanced-only permissions into the removable spawner item.
            if (old == null || !state.equals(old)) {
                BlockItem.setBlockEntityData(stack, BlockEntityType.MOB_SPAWNER, state);
                persist();
            }
        } finally { ticking = false; }
    }
    public void persist() {
        upgrade.getOrCreateTag().put("spawnerInventory", inventory.serializeNBT());
        save();
    }
}
