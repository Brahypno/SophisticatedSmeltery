package org.brahypno.sophisticated_smeltery.spawner;

import java.util.Optional;
import java.util.WeakHashMap;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.brahypno.sophisticated_smeltery.SophisticatedSmeltery;

@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID)
public final class SpawnerRuntime {
    private record Context(BaseSpawner spawner, boolean advanced, @Nullable IBackpackWrapper backpack) { }
    private record Origin(BlockPos pos, @Nullable IBackpackWrapper backpack) { }
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
    public static void tick(ServerLevel level, BlockPos pos, BaseSpawner spawner, boolean advanced, @Nullable IBackpackWrapper backpack) {
        Context previous = ACTIVE.get();
        ACTIVE.set(new Context(spawner, advanced, backpack));
        try { spawner.serverTick(level, pos); }
        finally { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void prepare(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawner() == null || event.isSpawnCancelled()) return;
        Context active = ACTIVE.get();
        var tile = event.getSpawner().getSpawnerBlockEntity();
        if (active != null && active.spawner == event.getSpawner()) {
            SPAWNS.put(event.getEntity(), new Origin(tile == null ? event.getEntity().blockPosition() : tile.getBlockPos(), active.backpack));
        } else if (tile != null) {
            SPAWNS.put(event.getEntity(), new Origin(tile.getBlockPos(), null));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Origin origin = SPAWNS.remove(event.getEntity());
        if (origin == null || event.loadedFromDisk()) return;
        if (origin.backpack != null) {
            if (capture(origin.backpack, event.getEntity(), level)) event.setCanceled(true);
            return;
        }
        for (Direction side : Direction.values()) {
            BlockPos pos = origin.pos.relative(side);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack
                    && capture(backpack.getBackpackWrapper(), event.getEntity(), level)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /** Native footprint/cost/storage APIs; automatic capture has no player or ownership grant. */
    private static boolean capture(IBackpackWrapper backpack, Entity entity, ServerLevel level) {
        var upgrade = MobCatcherHandler.getBestUpgrade(backpack);
        if (upgrade.isEmpty() || !upgrade.get().isEnabled() || !(entity instanceof LivingEntity mob)
                || mob instanceof Player || mob instanceof EnderDragon || mob instanceof WitherBoss
                || mob.isPassenger() || mob.isVehicle() || mob.isRemoved() || !mob.isAlive()
                || mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null
                || Config.SERVER.mobCatcherUpgrade.matchesEntityBlockList(mob.getType())
                || Config.SERVER.mobCatcherUpgrade.disallowInventoryEntities.get() && mob instanceof Container) return false;
        boolean hostile = MobCatcherHandler.isHostile(mob);
        if (hostile && !upgrade.get().isAdvanced()) return false;
        int cost = MobCatcherHandler.getSlotCost(mob, hostile);
        int limit = (upgrade.get().isAdvanced() ? Config.SERVER.mobCatcherUpgrade.advancedMaxSlotCost
                : Config.SERVER.mobCatcherUpgrade.basicMaxSlotCost).get();
        if (cost > limit) return false;
        var footprint = MobCatcherStorage.getFootprint(mob, cost);
        var slot = MobCatcherStorage.findEmptyRectangle(backpack, footprint);
        if (slot.isEmpty()) return false;
        CompoundTag tag = new CompoundTag();
        mob.saveWithoutId(tag);
        MobCatcherStorage.addCapturedMob(backpack, new CapturedMob(UUID.randomUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
                tag, slot.get(), footprint.width(), footprint.height(), cost, hostile, mob.getName().getString(),
                (int)Math.ceil(mob.getHealth()), (int)Math.ceil(Math.max(mob.getAttributeValue(Attributes.MAX_HEALTH), Math.max(mob.getMaxHealth(), mob.getHealth())))));
        for (var player : level.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof StorageContainerMenuBase<?> menu
                    && menu.getStorageWrapper() instanceof IBackpackWrapper viewed
                    && backpack.getContentsUuid().equals(viewed.getContentsUuid())) {
                MobCatcherHandler.syncCapturedMobs(player, backpack);
                break;
            }
        }
        return true;
    }
}
