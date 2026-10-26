package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import org.brahypno.sophisticated_smeltery.spawner.SpawnerRuntime;
import org.brahypno.sophisticated_smeltery.mixin.BlockEntityPositionAccess;
import org.brahypno.sophisticated_smeltery.mixin.SpawnerDelayAccess;

/** Tick the registered native tile without placing a block or replacing world contents. */
public final class SpawnerUpgradeWrapper extends UpgradeWrapperBase<SpawnerUpgradeWrapper, SpawnerUpgradeItem> implements ITickableUpgrade {
    private final ItemStackHandler inventory;
    private SpawnerBlockEntity spawner;
    private boolean loading = true;
    private boolean ticking;
    private boolean simpleCountdown;

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
        boolean created = spawner == null || spawner.getLevel() != level;
        if (created) {
            spawner = BlockEntityType.MOB_SPAWNER.create(pos, Blocks.SPAWNER.defaultBlockState());
            if (spawner == null) return;
            // Apotheosis replaces this factory; its own tile and logic are retained.
            spawner.setLevel(level);
            CompoundTag state = BlockItem.getBlockEntityData(inventory.getStackInSlot(0));
            if (state != null) spawner.load(state.copy());
            // Both supported implementations only update Delay while counting down.
            // Other registered factories retain full native serialization on every tick.
            simpleCountdown = spawner.getClass() == SpawnerBlockEntity.class || spawner.getClass().getName()
                    .equals("dev.shadowsoffire.apotheosis.spawn.spawner.ApothSpawnerTile");
        } else if (!spawner.getBlockPos().equals(pos)) {
            ((BlockEntityPositionAccess)spawner).sophisticatedsmeltery$setPosition(pos.immutable());
        }
        ticking = true;
        try {
            int previousDelay = ((SpawnerDelayAccess)spawner.getSpawner()).sophisticatedsmeltery$getSpawnDelay();
            SpawnerRuntime.tick(server, pos, spawner.getSpawner(), isAdvanced(),
                    storageWrapper);
            if (!created && simpleCountdown && previousDelay > 0) {
                int delay = ((SpawnerDelayAccess)spawner.getSpawner()).sophisticatedsmeltery$getSpawnDelay();
                if (delay != previousDelay) persistDelay(delay);
                return;
            }
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
    private void persistDelay(int delay) {
        CompoundTag state = BlockItem.getBlockEntityData(inventory.getStackInSlot(0));
        var savedItems = upgrade.getOrCreateTag().getCompound("spawnerInventory").getList("Items", Tag.TAG_COMPOUND);
        // A complete snapshot already exists after insertion and the first native tick.
        state.putShort("Delay", (short)delay);
        savedItems.getCompound(0).getCompound("tag").getCompound("BlockEntityTag").putShort("Delay", (short)delay);
        // Keep the normal host save notification: removing, copying or unloading must retain this exact tick's delay.
        save();
    }
    public void persist() {
        upgrade.getOrCreateTag().put("spawnerInventory", inventory.serializeNBT());
        save();
    }
}
