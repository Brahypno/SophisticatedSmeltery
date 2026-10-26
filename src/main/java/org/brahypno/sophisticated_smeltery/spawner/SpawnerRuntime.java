package org.brahypno.sophisticated_smeltery.spawner;

import java.util.Optional;
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
    private record Context(BaseSpawner spawner, boolean advanced, @Nullable IStorageWrapper storage) { }
    private record Origin(BlockPos pos, @Nullable IStorageWrapper storage) { }
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();
    // A cancelled spawn may never reach the join event; do not retain its detached entity.
    private static final WeakHashMap<Entity, Origin> SPAWNS = new WeakHashMap<>();
    private static final SpawnData.CustomSpawnRules UNRESTRICTED = new SpawnData.CustomSpawnRules(
            new InclusiveRange<>(0, 15), new InclusiveRange<>(0, 15));

    private SpawnerRuntime() { }
    public static boolean ignoresPlayer(BaseSpawner spawner) {
        Context active = ACTIVE.get();
        return active != null && active.advanced && active.spawner == spawner;
    }
    public static Optional<SpawnData.CustomSpawnRules> rules(Optional<SpawnData.CustomSpawnRules> original) {
        Context active = ACTIVE.get();
        return active != null && active.advanced ? Optional.of(UNRESTRICTED) : original;
    }
    public static void tick(ServerLevel level, BlockPos pos, BaseSpawner spawner, boolean advanced, @Nullable IStorageWrapper storage) {
        Context previous = ACTIVE.get();
        ACTIVE.set(new Context(spawner, advanced, storage));
        try { spawner.serverTick(level, pos); }
        finally { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
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
        for (Direction side : Direction.values()) {
            BlockPos pos = origin.pos.relative(side);
            if (level.hasChunkAt(pos) && BackpackMobCapture.captureBlock(level.getBlockEntity(pos), event.getEntity(), level)) {
                event.setCanceled(true);
                return;
            }
        }
    }

}
