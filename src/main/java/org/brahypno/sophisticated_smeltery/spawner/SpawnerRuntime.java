package org.brahypno.sophisticated_smeltery.spawner;

import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.minecraftforge.fml.ModList;
import org.brahypno.sophisticated_smeltery.compat.BackpackMobCapture;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID)
public final class SpawnerRuntime {
    private static final class Context {
        final BaseSpawner spawner;
        final boolean advanced;
        @Nullable final IStorageWrapper storage;
        @Nullable final ServerLevel world;
        @Nullable final BlockPos pos;
        @Nullable Boolean adjacentAdvanced;
        @Nullable Set<IStorageWrapper> captured;

        Context(BaseSpawner spawner, boolean advanced, @Nullable IStorageWrapper storage, @Nullable ServerLevel world, @Nullable BlockPos pos) {
            this.spawner = spawner;
            this.advanced = advanced;
            this.storage = storage;
            this.world = world;
            this.pos = pos;
        }

        boolean isAdvanced() {
            if (world == null) return advanced;
            if (adjacentAdvanced == null) adjacentAdvanced = hasAdjacentAdvancedUpgrade(world, pos);
            return adjacentAdvanced;
        }
    }
    private record Origin(BlockPos pos, @Nullable IStorageWrapper storage) { }
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();
    private static final Direction[] SIDES = Direction.values();
    // A cancelled spawn may never reach the join event; do not retain its detached entity.
    private static final WeakHashMap<Entity, Origin> SPAWNS = new WeakHashMap<>();
    private static final SpawnData.CustomSpawnRules UNRESTRICTED = new SpawnData.CustomSpawnRules(
            new InclusiveRange<>(0, 15), new InclusiveRange<>(0, 15));

    private SpawnerRuntime() { }
    public static boolean ignoresPlayer(BaseSpawner spawner) {
        Context active = ACTIVE.get();
        return active != null && active.advanced && active.spawner == spawner;
    }
    public static boolean hasAdvancedSupport(BaseSpawner spawner) {
        Context active = ACTIVE.get();
        return active != null && active.spawner == spawner && active.isAdvanced();
    }
    public static Optional<SpawnData.CustomSpawnRules> rules(Optional<SpawnData.CustomSpawnRules> original) {
        Context active = ACTIVE.get();
        return active != null && active.isAdvanced() ? Optional.of(UNRESTRICTED) : original;
    }
    public static void tick(ServerLevel level, BlockPos pos, BaseSpawner spawner, boolean advanced, @Nullable IStorageWrapper storage) {
        tick(level, pos, new Context(spawner, advanced, storage, null, null));
    }

    private static void tick(ServerLevel level, BlockPos pos, Context context) {
        Context previous = ACTIVE.get();
        ACTIVE.set(context);
        try { context.spawner.serverTick(level, pos); }
        finally {
            if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
            if (context.captured != null) for (IStorageWrapper storage : context.captured)
                BackpackMobCapture.syncViewers(level, storage);
        }
    }

    public static void tickWorld(ServerLevel level, BlockPos pos, BaseSpawner spawner) {
        // Keep this a world spawn: the existing event path chooses any eligible adjacent catcher.
        if (!ModList.get().isLoaded("sophisticatedbackpacks")) { spawner.serverTick(level, pos); return; }
        tick(level, pos, new Context(spawner, false, null, level, pos));
    }

    /** Capture storage is updated immediately; viewers receive one final snapshot per native spawn batch. */
    public static boolean deferCaptureSync(IStorageWrapper storage) {
        Context active = ACTIVE.get();
        if (active == null) return false;
        if (active.captured == null) active.captured = new LinkedHashSet<>();
        active.captured.add(storage);
        return true;
    }

    private static boolean hasAdjacentAdvancedUpgrade(ServerLevel level, BlockPos origin) {
        if (!ModList.get().isLoaded("sophisticatedbackpacks")) return false;
        for (Direction side : SIDES) {
            BlockPos pos = origin.relative(side);
            if (level.hasChunkAt(pos) && BackpackMobCapture.hasAdvancedSpawnerSupport(level.getBlockEntity(pos))) return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void prepare(MobSpawnEvent.FinalizeSpawn event) {
        if (!ModList.get().isLoaded("sophisticatedbackpacks") || event.getSpawner() == null || event.isSpawnCancelled()) return;
        Context active = ACTIVE.get();
        var tile = event.getSpawner().getSpawnerBlockEntity();
        if (active != null && active.spawner == event.getSpawner()) {
            SPAWNS.put(event.getEntity(), new Origin(tile == null ? event.getEntity().blockPosition() : tile.getBlockPos(), active.storage));
        } else if (tile != null) {
            SPAWNS.put(event.getEntity(), new Origin(tile.getBlockPos(), null));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !ModList.get().isLoaded("sophisticatedbackpacks")) return;
        Origin origin = SPAWNS.remove(event.getEntity());
        if (origin == null || event.loadedFromDisk()) return;
        if (origin.storage != null) {
            if (BackpackMobCapture.captureStorage(origin.storage, event.getEntity(), level)) event.setCanceled(true);
            return;
        }
        for (Direction side : SIDES) {
            BlockPos pos = origin.pos.relative(side);
            if (level.hasChunkAt(pos) && BackpackMobCapture.captureBlock(level.getBlockEntity(pos), event.getEntity(), level)) {
                event.setCanceled(true);
                return;
            }
        }
    }

}
