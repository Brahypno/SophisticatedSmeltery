package org.brahypno.sophisticated_smeltery;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.furnace.PortableMobMelting;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.block.entity.module.EntityMeltingModule;
import slimeknights.tconstruct.smeltery.item.TankItem;

@PrefixGameTestTemplate(false)
public final class MobMeltingGameTests {
    private static int[] furnaces() { return ModList.get().isLoaded("esotericism_tinker") ? new int[]{0, 1, 2} : new int[]{0, 1}; }

    private static ItemStack transmuteItem(String name, int count) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("esotericism_tinker", name)), count);
    }

    private static BackpackWrapper backpack(boolean foundry, boolean fuel) {
        return backpack(foundry ? 1 : 0, fuel);
    }

    private static BackpackWrapper backpack(int kind, boolean fuel) {
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, ControllerBinding.bind(
                new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()),
                kind == 2 ? transmuteItem("transmute_controller", 1)
                        : new ItemStack(kind == 1 ? TinkerSmeltery.foundryController : TinkerSmeltery.smelteryController)));
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.ADVANCED_MOB_CATCHER_UPGRADE.get()));
        var upgrade = melting(storage);
        int count = 1;
        while (upgrade.getFurnace().calculate(count + 1).volume() == 0) count++;
        upgrade.getComponents().setStackInSlot(0, kind == 2 ? transmuteItem("ashen_bricks", count)
                : new ItemStack(kind == 1 ? TinkerSmeltery.scorchedBricks : TinkerSmeltery.searedBricks, count));
        if (fuel) addFuel(upgrade);
        return storage;
    }

    private static MeltingUpgradeWrapper melting(BackpackWrapper storage) {
        return (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
    }

    private static void addFuel(MeltingUpgradeWrapper upgrade) {
        ItemStack tank = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK));
        if (upgrade.getFurnace().controller() instanceof slimeknights.tconstruct.smeltery.block.entity.controller.FoundryBlockEntity) {
            tank = new ItemStack(TinkerSmeltery.scorchedTank.get(SearedTankBlock.TankType.FUEL_TANK));
        } else if (upgrade.getFurnace().controller().getClass().getName().endsWith(".TransmuteBlockEntity")) {
            tank = transmuteItem("ashen_fuel_tank", 1);
        }
        TankItem.setTank(tank, new FluidStack(Fluids.LAVA, 1000));
        upgrade.getComponents().setStackInSlot(1, tank);
    }

    private static CapturedMob capture(IBackpackWrapper storage, LivingEntity entity, int slot, int width, int height) {
        CompoundTag nbt = new CompoundTag();
        entity.saveWithoutId(nbt);
        var mob = new CapturedMob(UUID.randomUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), nbt,
                slot, width, height, width * height, false, entity.getName().getString(),
                (int)Math.ceil(entity.getHealth()), (int)Math.ceil(entity.getMaxHealth()));
        MobCatcherStorage.addCapturedMob(storage, mob);
        return mob;
    }

    private static void atPulse(GameTestHelper helper, Runnable action) {
        helper.onEachTick(() -> {
            if (helper.getLevel().getGameTime() % 20 != 12) return;
            action.run();
            helper.succeed();
        });
    }

    private static void nativeTransmute(GameTestHelper helper, MeltingUpgradeWrapper upgrade, LivingEntity mob,
                                        net.minecraftforge.fluids.capability.IFluidHandler tank) {
        try {
            upgrade.getFurnace().controller().setLevel(helper.getLevel());
            Class<?> type = Class.forName("org.brahypno.esotericismtinker.smeltery.block.entity.module.ByproductEntityMeltingModule");
            var module = type.getConstructor(slimeknights.mantle.block.entity.MantleBlockEntity.class,
                    net.minecraftforge.fluids.capability.IFluidHandler.class, java.util.function.BooleanSupplier.class,
                    java.util.function.Function.class, java.util.function.Supplier.class).newInstance(
                    upgrade.getFurnace().controller(), tank, (java.util.function.BooleanSupplier)() -> true,
                    (java.util.function.Function<ItemStack, ItemStack>)stack -> stack,
                    (java.util.function.Supplier<net.minecraft.world.phys.AABB>)mob::getBoundingBox);
            type.getMethod("interactWithEntities").invoke(module);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static void reloadRecipeCaches(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var modifiers = slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup.getRecipeModifierList();
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            new net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket(manager.getRecipes()).write(buffer);
            slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator.reload(false);
            // Real reload reconstructs recipes, registering fuel, modifier slots,
            // salvage and other native lookups. Reusing recipe instances after
            // a global clear leaves unrelated worktable extraction lists empty.
            var restored = new net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket(buffer);
            manager.replaceRecipes(restored.getRecipes());
            helper.assertTrue(modifiers.equals(slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup.getRecipeModifierList()),
                    "Simulated reload preserves native modifier recipe registrations");
        } finally {
            buffer.release();
        }
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, timeoutTicks = 100, batch = "estinker_mobs")
    public static void transmuteCustomPriorityByproductsCapacityHeatersAndReload(GameTestHelper helper) {
        if (!ModList.get().isLoaded("esotericism_tinker")) { helper.succeed(); return; }
        atPulse(helper, () -> {
            var manager = helper.getLevel().getRecipeManager();
            var original = new ArrayList<>(manager.getRecipes());
            try {
                var serializer = ForgeRegistries.RECIPE_SERIALIZERS.getValue(new ResourceLocation("esotericism_tinker", "byproduct_entity_melting"));
                var recipes = new ArrayList<>(original);
                for (int priority : new int[]{1, 10}) {
                    var json = com.google.gson.JsonParser.parseString("{\"entity\":{\"type\":\"minecraft:cow\"},"
                            + "\"result\":{\"fluid\":\"tconstruct:molten_iron\",\"amount\":" + (priority == 10 ? 90 : 1)
                            + "},\"damage\":3,\"priority\":" + priority
                            + ",\"byproducts\":[{\"fluid\":\"tconstruct:molten_copper\",\"amount\":30},{\"fluid\":\"tconstruct:molten_gold\",\"amount\":20}]}").getAsJsonObject();
                    recipes.add(serializer.fromJson(SophisticatedSmeltery.id("mob_priority_" + priority), json));
                }
                manager.replaceRecipes(recipes);
                reloadRecipeCaches(helper);
                var storage = backpack(2, true);
                var upgrade = melting(storage);
                var cow = EntityType.COW.create(helper.getLevel());
                cow.setHealth(10);
                cow.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
                var captured = capture(storage, cow, 0, 2, 2);
                helper.getLevel().addFreshEntity(cow);
                var nativeController = ControllerBinding.create(transmuteItem("transmute_controller", 1));
                nativeController.getTank().setCapacity(10000);
                nativeTransmute(helper, upgrade, cow, nativeController.getTank());
                upgrade.tick(null, helper.getLevel(), BlockPos.ZERO);
                helper.assertTrue(MobCatcherStorage.getCapturedMob(storage, captured.id()).orElseThrow().currentHealth() == cow.getHealth()
                        && cow.getHealth() == 7, "Use the native custom recipe damage and priority");
                helper.assertTrue(upgrade.getTank().getFluids().equals(nativeController.getTank().getFluids())
                        && upgrade.getTank().getContained() == 140 && upgrade.getTank().getFluids().size() == 3,
                        "Use the native main and all byproducts exactly, without the fallback multiplier");
                cow.discard();
                var restored = new MeltingUpgradeWrapper(storage, upgrade.getUpgradeStack().copy(), ignored -> {});
                helper.assertTrue(restored.getTank().getFluids().equals(upgrade.getTank().getFluids()), "Save every fluid immediately");
                upgrade.getTank().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
                upgrade.getTank().fill(new FluidStack(Fluids.WATER, upgrade.getTank().getCapacity() - 90), FluidAction.EXECUTE);
                upgrade.tick(null, helper.getLevel(), BlockPos.ZERO);
                helper.assertTrue(MobCatcherStorage.getCapturedMob(storage, captured.id()).orElseThrow().currentHealth() == 7,
                        "Captured-mob capacity protection includes all byproducts before damage");

                var heated = backpack(2, false);
                var heatedUpgrade = melting(heated);
                heatedUpgrade.getComponents().setStackInSlot(1, transmuteItem("ashen_fuel_tank", 1));
                heatedUpgrade.getComponents().setStackInSlot(2, transmuteItem("ashen_heater", 1));
                var target = capture(heated, EntityType.COW.create(helper.getLevel()), 0, 2, 2);
                heatedUpgrade.tick(null, helper.getLevel(), BlockPos.ZERO);
                helper.assertTrue(MobCatcherStorage.getCapturedMob(heated, target.id()).orElseThrow().currentHealth() == 7
                        && heatedUpgrade.getTank().getContained() == 140 && !heatedUpgrade.getFurnace().fuel().hasFuel(),
                        "Native heater heat powers mob melting without burning fuel");
                // Removing custom recipes must restore the native fallback and its configured multiplier.
                manager.replaceRecipes(original);
                reloadRecipeCaches(helper);
                heatedUpgrade.getTank().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
                heatedUpgrade.tick(null, helper.getLevel(), BlockPos.ZERO);
                helper.assertTrue(MobCatcherStorage.getCapturedMob(heated, target.id()).orElseThrow().currentHealth() == 5
                        && heatedUpgrade.getTank().getContained() > 0, "Recipe reload restores fallback damage and output");
                SophisticatedSmeltery.LOGGER.info("TRANSMUTE MOB MELTING PASSED: native custom priority/damage/main/byproducts, complete capacity, heater-only heat, save and recipe reload");
            } finally {
                manager.replaceRecipes(original);
                reloadRecipeCaches(helper);
            }
        });
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, timeoutTicks = 100)
    public static void nativeDamageAndFluidsWithPartialOverlap(GameTestHelper helper) {
        atPulse(helper, () -> {
            for (int kind : furnaces()) {
                var storage = backpack(kind, true);
                var upgrade = melting(storage);
                // A tall footprint crosses the prefix boundary; its top-left occupied slot is sufficient.
                for (var type : new EntityType<?>[]{EntityType.COW, EntityType.IRON_GOLEM, EntityType.BLAZE}) {
                    var nativeMob = (LivingEntity)type.create(helper.getLevel());
                    nativeMob.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
                    nativeMob.setHealth(20);
                    var captured = capture(storage, nativeMob, 0, 2, 2);
                    helper.getLevel().addFreshEntity(nativeMob);
                    FluidTank nativeTank = new FluidTank(10000);
                    upgrade.getFurnace().controller().setLevel(helper.getLevel());
                    if (kind == 2) nativeTransmute(helper, upgrade, nativeMob, nativeTank);
                    else new EntityMeltingModule(upgrade.getFurnace().controller(), nativeTank,
                            () -> true, stack -> stack, nativeMob::getBoundingBox).interactWithEntities();
                    upgrade.tick(null, helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
                    var updated = MobCatcherStorage.getCapturedMob(storage, captured.id()).orElseThrow();
                    helper.assertTrue(updated.entityNbt().getFloat("Health") == nativeMob.getHealth()
                                    && updated.currentHealth() == (int)Math.ceil(nativeMob.getHealth()),
                            "Captured damage and display health must match the native module: " + type);
                    helper.assertTrue(upgrade.getTank().getContained() == nativeTank.getFluidAmount()
                                    && upgrade.getTank().getFluidInTank(0).isFluidEqual(nativeTank.getFluid()),
                            "Exact native fluid per successful hit, including default soul and fire-immune mobs");
                    helper.assertTrue(upgrade.getFurnace().fuel().hasFuel(), "Mob-only processing must start actual fuel");
                    helper.assertTrue(!storage.getInventoryHandler().isSlotAccessible(0), "Surviving mob retains its occupied slots");
                    nativeMob.discard();
                    MobCatcherStorage.removeCapturedMob(storage, captured.id());
                    upgrade.getTank().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
                }
            }
        });
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, timeoutTicks = 100)
    public static void heatingFuelCapacityImmunityAndOutsideSlots(GameTestHelper helper) {
        atPulse(helper, () -> {
            for (int kind : furnaces()) {
                var storage = backpack(kind, false);
                var upgrade = melting(storage);
                var cow = EntityType.COW.create(helper.getLevel());
                cow.setHealth(10);
                var target = capture(storage, cow, 0, 2, 2);
                var outside = capture(storage, cow, 20, 1, 1);
                cow.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000));
                var resistant = capture(storage, cow, 30, 1, 1);
                cow.removeAllEffects();
                cow.setInvulnerable(true);
                var invulnerable = capture(storage, cow, 31, 1, 1);
                var boss = EntityType.WITHER.create(helper.getLevel());
                helper.assertTrue(boss.getType().is(TinkerTags.EntityTypes.MELTING_BLACKLIST), "Native blacklist fixture");
                var blacklisted = capture(storage, boss, 32, 1, 1);
                upgrade.tick(null, helper.getLevel(), BlockPos.ZERO);
                helper.assertTrue(MobCatcherStorage.getCapturedMob(storage, target.id()).orElseThrow().currentHealth() == 10,
                        "No fuel means no damage");
                addFuel(upgrade);
                upgrade.setHeating(false);
                upgrade.tick(null, helper.getLevel(), BlockPos.ZERO);
                helper.assertTrue(upgrade.getTank().getContained() == 0, "The shared heating switch disables mob processing");
                upgrade.setHeating(true);
                upgrade.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), upgrade.getTank().getCapacity() - 1), FluidAction.EXECUTE);
                upgrade.tick(null, helper.getLevel(), BlockPos.ZERO);
                helper.assertTrue(MobCatcherStorage.getCapturedMob(storage, target.id()).orElseThrow().currentHealth() == 10
                                && !upgrade.getFurnace().fuel().hasFuel(), "Insufficient space must not damage or start fuel");
                upgrade.getTank().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
                upgrade.tick(null, helper.getLevel(), BlockPos.ZERO);
                helper.assertTrue(MobCatcherStorage.getCapturedMob(storage, target.id()).orElseThrow().currentHealth() < 10,
                        "Automatic processing resumes when space exists");
                for (var untouched : new CapturedMob[]{outside, resistant, invulnerable, blacklisted}) {
                    helper.assertTrue(MobCatcherStorage.getCapturedMob(storage, untouched.id()).orElseThrow().entityNbt().equals(untouched.entityNbt()),
                            "Outside or immune captured records remain unchanged");
                }
                for (var immune : new CapturedMob[]{resistant, invulnerable, blacklisted}) {
                    var isolated = backpack(kind, true);
                    var entity = (LivingEntity)MobCatcherStorage.getEntityType(immune).orElseThrow().create(helper.getLevel());
                    entity.load(immune.entityNbt().copy());
                    var inRange = capture(isolated, entity, 0, 1, 1);
                    melting(isolated).tick(null, helper.getLevel(), BlockPos.ZERO);
                    helper.assertTrue(MobCatcherStorage.getCapturedMob(isolated, inRange.id()).orElseThrow().entityNbt().equals(inRange.entityNbt())
                                    && melting(isolated).getTank().getContained() == 0 && !melting(isolated).getFurnace().fuel().hasFuel(),
                            "An in-range immune or blacklisted mob must not take damage, produce fluid, or ignite fuel");
                }
            }
        });
    }

    public static final class LootProbe {
        final UUID marker = UUID.randomUUID();
        int deaths;
        boolean cancelHurt;
        boolean cancelDrops;
        boolean matches(LivingEntity entity) { return entity.getPersistentData().hasUUID("mobMeltingTest")
                && entity.getPersistentData().getUUID("mobMeltingTest").equals(marker); }
        @SubscribeEvent public void death(LivingDeathEvent event) { if (matches(event.getEntity())) deaths++; }
        @SubscribeEvent public void hurt(LivingAttackEvent event) { if (matches(event.getEntity()) && cancelHurt) event.setCanceled(true); }
        @SubscribeEvent public void drops(LivingDropsEvent event) {
            if (!matches(event.getEntity())) return;
            if (cancelDrops) { event.setCanceled(true); return; }
            event.getDrops().clear();
            for (int index = 0; index < 2; index++)
                event.getDrops().add(new ItemEntity(event.getEntity().level(), 0, 0, 0, new ItemStack(Items.DIAMOND, 64)));
        }
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, timeoutTicks = 100)
    public static void realDeathDropsOverflowReloadAndRefund(GameTestHelper helper) {
        atPulse(helper, () -> {
            var probe = new LootProbe();
            MinecraftForge.EVENT_BUS.register(probe);
            try {
                for (int kind : furnaces()) {
                    var storage = backpack(kind, true);
                    var upgrade = melting(storage);
                    var cow = EntityType.COW.create(helper.getLevel());
                    cow.setHealth(1);
                    cow.getPersistentData().putUUID("mobMeltingTest", probe.marker);
                    capture(storage, cow, 0, 1, 1);
                    for (int slot = 1; slot < storage.getInventoryHandler().getSlots(); slot++)
                        storage.getInventoryHandler().setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
                    probe.cancelHurt = true;
                    upgrade.tick(null, helper.getLevel(), BlockPos.ZERO);
                    helper.assertTrue(upgrade.getTank().getContained() == 0 && probe.deaths == 0,
                            "Cancelled actual damage produces neither fluid nor death");
                    probe.cancelHurt = false;
                    upgrade.tick(null, helper.getLevel(), BlockPos.ZERO);
                    helper.assertTrue(probe.deaths == 1 && MobCatcherStorage.getCapturedMobs(storage).isEmpty(),
                            "Real death event fires once and removes the captured record");
                    helper.assertTrue(storage.getInventoryHandler().isSlotAccessible(0)
                                    && storage.getInventoryHandler().getStackInSlot(0).is(Items.DIAMOND),
                            "Death frees the occupied slot and its loot can use that slot");
                    ItemStack saved = storage.getUpgradeHandler().getStackInSlot(0).copy();
                    helper.assertTrue(saved.getTag().getCompound(ControllerBinding.STATE).getList(PortableMobMelting.DROPS, Tag.TAG_COMPOUND).size() == 1,
                            "Full inventory preserves the remaining actual event-modified loot");
                    helper.assertTrue(ControllerBinding.getStoredItems(saved).stream().filter(stack -> stack.is(Items.DIAMOND))
                            .mapToInt(ItemStack::getCount).sum() == 64, "Binding/clearing refunds include pending loot");
                    var restored = new MeltingUpgradeWrapper(storage, saved, ignored -> {});
                    restored.setHeating(false);
                    storage.getInventoryHandler().setStackInSlot(0, ItemStack.EMPTY);
                    storage.getInventoryHandler().setStackInSlot(10, ItemStack.EMPTY);
                    restored.tick(null, helper.getLevel(), BlockPos.ZERO);
                    restored.tick(null, helper.getLevel(), BlockPos.ZERO);
                    helper.assertTrue(storage.getInventoryHandler().getStackInSlot(10).is(Items.DIAMOND)
                                    && storage.getInventoryHandler().getStackInSlot(10).getCount() == 64
                                    && storage.getInventoryHandler().getStackInSlot(0).isEmpty() && probe.deaths == 1,
                            "Reload delivers once while off, prefers ordinary slots, and cannot rerun the death");
                    helper.assertTrue(saved.getTag().getCompound(ControllerBinding.STATE).getList(PortableMobMelting.DROPS, Tag.TAG_COMPOUND).isEmpty(),
                            "Delivery immediately saves the empty pending queue");

                    var cancelled = backpack(kind, true);
                    capture(cancelled, cow, 0, 1, 1);
                    probe.cancelDrops = true;
                    melting(cancelled).tick(null, helper.getLevel(), BlockPos.ZERO);
                    helper.assertTrue(cancelled.getInventoryHandler().getStackInSlot(0).isEmpty()
                                    && MobCatcherStorage.getCapturedMobs(cancelled).isEmpty(), "Respect another mod cancelling drops");
                    probe.deaths = 0;
                    probe.cancelDrops = false;
                }
            } finally { MinecraftForge.EVENT_BUS.unregister(probe); }
        });
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, timeoutTicks = 120)
    public static void actualPlacedAndCarriedHostsProcessNewCaptures(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        var hosts = new ArrayList<IBackpackWrapper>();
        for (int kind : furnaces()) {
            var placed = backpack(kind, true);
            ItemStack held = backpack(kind, true).getBackpack();
            BlockPos pos = new BlockPos(0, kind * 2, 0);
            helper.setBlock(pos, ModBlocks.NETHERITE_BACKPACK.get());
            var block = (BackpackBlockEntity)helper.getBlockEntity(pos);
            block.setBackpack(placed.getBackpack());
            hosts.add(block.getBackpackWrapper());
            hosts.add(held.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new));
            helper.onEachTick(() -> ((BackpackItem)held.getItem()).inventoryTick(held, helper.getLevel(), player, -1, false));
        }
        for (var storage : hosts) {
            var cow = EntityType.COW.create(helper.getLevel());
            cow.setHealth(10);
            capture(storage, cow, 0, 2, 2);
            helper.assertTrue(MobCatcherStorage.getCapturedMobs(storage).size() == 1, "Capture into the actual resolved host");
        }
        helper.succeedWhen(() -> {
            for (var storage : hosts) {
                var records = MobCatcherStorage.getCapturedMobs(storage);
                helper.assertTrue(!records.isEmpty(), "Actual host must retain the captured record: " + storage.getContentsUuid());
                var mob = records.get(0);
                helper.assertTrue(mob.currentHealth() < 10 && mob.currentHealth() > 0, "Actual placed and carried ticks both damage captured mobs");
                var actual = (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
                var restored = new MeltingUpgradeWrapper(storage, actual.getUpgradeStack().copy(), ignored -> {});
                helper.assertTrue(actual.getTank().getContained() == restored.getTank().getContained()
                                && actual.getTank().getContained() > 0, "Host saves the resulting fluid immediately");
                var released = EntityType.COW.create(helper.getLevel());
                released.load(mob.entityNbt().copy());
                helper.assertTrue(released.getHealth() == mob.entityNbt().getFloat("Health") && released.getHealth() < 10,
                        "Recreating the mob for release retains its damaged health");
            }
        });
    }
}
