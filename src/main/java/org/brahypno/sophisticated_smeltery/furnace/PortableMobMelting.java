package org.brahypno.sophisticated_smeltery.furnace;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.brahypno.sophisticated_smeltery.compat.TransmuteMobMelting;
import org.brahypno.sophisticated_smeltery.mixin.MobCatcherStorageAccess;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipeCache;
import slimeknights.tconstruct.smeltery.block.entity.module.EntityMeltingModule;

/** Captured entities stay in catcher storage; only a detached damage/loot instance is created. */
public final class PortableMobMelting implements MobMelting {
    public static final String DROPS = "mobDrops";
    public record Melt(int damage, List<FluidStack> fluids) {}
    private record Capture(LivingEntity entity, List<ItemStack> drops) {}
    private static final ThreadLocal<Capture> CAPTURE = new ThreadLocal<>();
    private final MeltingUpgradeWrapper upgrade;
    private final List<ItemStack> pendingDrops = new ArrayList<>();

    public PortableMobMelting(MeltingUpgradeWrapper upgrade, CompoundTag state) {
        this.upgrade = upgrade;
        for (Tag tag : state.getList(DROPS, Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.of((CompoundTag)tag);
            if (!stack.isEmpty()) pendingDrops.add(stack);
        }
    }

    public void save(CompoundTag state) {
        ListTag drops = new ListTag();
        pendingDrops.forEach(stack -> drops.add(stack.save(new CompoundTag())));
        state.put(DROPS, drops);
    }

    /** Normal deaths and drops cancelled by another mod are left alone. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureDrops(LivingDropsEvent event) {
        Capture capture = CAPTURE.get();
        if (capture == null || capture.entity() != event.getEntity()) return;
        event.getDrops().forEach(drop -> capture.drops().add(drop.getItem().copy()));
        event.getDrops().clear();
    }

    public void tick(@Nullable Entity host, ServerLevel level, BlockPos pos) {
        NativeFurnace furnace = upgrade.getFurnace();
        if (furnace == null) return;
        boolean changed = flushDrops();
        boolean transmute = ModList.get().isLoaded("esotericism_tinker") && TransmuteMobMelting.supports(furnace.controller());
        if (upgrade.isHeating() && upgrade.getMeltingSlotCount() > 0 && level.getGameTime() % 20 == 12
                && upgrade.getStorage() instanceof IBackpackWrapper backpack) {
            List<CapturedMob> mobs = new ArrayList<>(MobCatcherStorage.getCapturedMobs(backpack));
            int meltingSlots = upgrade.getMeltingSlotCount();
            boolean mobsChanged = false;
            for (int index = 0; index < mobs.size(); index++) {
                CapturedMob mob = mobs.get(index);
                // The anchor is the first occupied slot, so any overlap with the prefix is equivalent.
                if (mob.slot() < 0 || mob.slot() >= meltingSlots) continue;
                var type = MobCatcherStorage.getEntityType(mob).orElse(null);
                if (type == null || type.is(TinkerTags.EntityTypes.MELTING_BLACKLIST)) continue;
                Entity entity = type.create(level);
                if (!(entity instanceof LivingEntity living)) continue;
                living.load(mob.entityNbt().copy());
                if (!living.isAlive() || living.hasEffect(MobEffects.FIRE_RESISTANCE)) continue;
                Vec3 origin = host == null ? Vec3.atCenterOf(pos) : host.position();
                living.setPos(origin);
                DamageSource source = TinkerDamageTypes.source(level.registryAccess(), living.fireImmune()
                        ? TinkerDamageTypes.SMELTERY_MAGIC : TinkerDamageTypes.SMELTERY_HEAT);
                if (living.isInvulnerableTo(source)) continue;
                Melt melt;
                if (transmute) melt = TransmuteMobMelting.recipe(level, living);
                else {
                    var recipe = EntityMeltingRecipeCache.findRecipe(level.getRecipeManager(), living.getType());
                    melt = new Melt(recipe == null ? 2 : recipe.getDamage(),
                            List.of(recipe == null ? EntityMeltingModule.getDefaultFluid() : recipe.getOutput(living)));
                }
                long amount = melt.fluids().stream().mapToLong(FluidStack::getAmount).sum();
                if (amount > upgrade.getTank().getCapacity() - upgrade.getTank().getContained()) continue;
                boolean heater = transmute && furnace.heatingTemperature() > 0;
                if (!heater && !furnace.fuel().hasFuel() && furnace.fuel().findFuel(false) <= 0) continue;

                // Captured mobs do not tick. A full second elapsed since the previous pulse.
                living.invulnerableTime = 0;
                living.hurtTime = 0;
                List<ItemEntity> extraDrops = new ArrayList<>();
                var oldDrops = living.captureDrops(extraDrops);
                Capture previous = CAPTURE.get();
                CAPTURE.set(new Capture(living, pendingDrops));
                boolean hurt;
                try {
                    hurt = living.hurt(source, melt.damage());
                } finally {
                    living.captureDrops(oldDrops);
                    if (previous == null) CAPTURE.remove();
                    else CAPTURE.set(previous);
                    extraDrops.forEach(drop -> pendingDrops.add(drop.getItem().copy()));
                }
                if (hurt) {
                    if (!heater && !furnace.fuel().hasFuel()) furnace.fuel().findFuel(true);
                    for (var fluid : melt.fluids()) upgrade.getTank().fill(fluid, FluidAction.EXECUTE);
                }
                if (!living.isAlive()) {
                    mobs.remove(index--);
                } else {
                    CompoundTag entityTag = new CompoundTag();
                    living.saveWithoutId(entityTag);
                    mobs.set(index, new CapturedMob(mob.id(), mob.entityType(), entityTag, mob.slot(), mob.width(),
                            mob.height(), mob.slotCost(), mob.hostile(), mob.displayName(),
                            (int)Math.ceil(living.getHealth()), mob.maxHealth()));
                }
                mobsChanged = true;
            }
            if (mobsChanged) {
                MobCatcherStorageAccess.sophisticatedsmeltery$save(backpack, mobs);
                syncViewers(level, backpack);
                changed = true;
                flushDrops(); // Dead mobs have now released their occupied slots.
            }
        }
        if (changed) upgrade.persist();
    }

    private static void syncViewers(ServerLevel level, IBackpackWrapper backpack) {
        for (var player : level.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof StorageContainerMenuBase<?> menu
                    && menu.getStorageWrapper() instanceof IBackpackWrapper viewed
                    && backpack.getContentsUuid().equals(viewed.getContentsUuid())) {
                // The native method also updates other players viewing the same backpack.
                MobCatcherHandler.syncCapturedMobs(player, backpack);
                return;
            }
        }
    }

    private boolean flushDrops() {
        if (pendingDrops.isEmpty()) return false;
        boolean changed = false;
        var inventory = upgrade.getStorage().getInventoryHandler();
        int meltingSlots = upgrade.getMeltingSlotCount();
        for (var iterator = pendingDrops.listIterator(); iterator.hasNext();) {
            ItemStack original = iterator.next();
            ItemStack remaining = original;
            for (int pass = 0; pass < 2; pass++) {
                int start = pass == 0 ? meltingSlots : 0;
                int end = pass == 0 ? inventory.getSlots() : meltingSlots;
                for (int slot = start; slot < end && !remaining.isEmpty(); slot++)
                    remaining = inventory.insertItemOnlyToSlot(slot, remaining, false);
            }
            if (remaining.getCount() != original.getCount()) changed = true;
            if (remaining.isEmpty()) iterator.remove();
            else iterator.set(remaining);
        }
        return changed;
    }
}
