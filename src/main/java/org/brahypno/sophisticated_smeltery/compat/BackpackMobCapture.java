package org.brahypno.sophisticated_smeltery.compat;

import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.brahypno.sophisticated_smeltery.upgrade.SpawnerUpgradeItem;
import org.brahypno.sophisticated_smeltery.spawner.SpawnerRuntime;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;

public final class BackpackMobCapture {
    private BackpackMobCapture() {}

    public static boolean hasAdvancedSpawnerSupport(net.minecraft.world.level.block.entity.BlockEntity block) {
        if (!(block instanceof BackpackBlockEntity backpack)) return false;
        for (var wrapper : backpack.getBackpackWrapper().getUpgradeHandler().getTypeWrappers(SpawnerUpgradeItem.TYPE))
            if (wrapper.isAdvanced() && wrapper.isEnabled()) return true;
        return false;
    }

    public static boolean captureStorage(net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper storage, Entity entity, ServerLevel level) {
        return storage instanceof IBackpackWrapper backpack && capture(backpack, entity, level);
    }

    public static boolean captureBlock(net.minecraft.world.level.block.entity.BlockEntity block, Entity entity, ServerLevel level) {
        return block instanceof BackpackBlockEntity backpack && capture(backpack.getBackpackWrapper(), entity, level);
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
        if (!SpawnerRuntime.deferCaptureSync(backpack)) syncViewers(level, backpack);
        return true;
    }

    public static void syncViewers(ServerLevel level, IStorageWrapper storage) {
        if (!(storage instanceof IBackpackWrapper backpack)) return;
        for (var player : level.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof StorageContainerMenuBase<?> menu
                    && menu.getStorageWrapper() instanceof IBackpackWrapper viewed
                    && backpack.getContentsUuid().equals(viewed.getContentsUuid())) {
                MobCatcherHandler.syncCapturedMobs(player, backpack);
                break;
            }
        }
    }
}
