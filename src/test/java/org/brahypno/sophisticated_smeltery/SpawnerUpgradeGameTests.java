package org.brahypno.sophisticated_smeltery;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import org.brahypno.sophisticated_smeltery.upgrade.SpawnerUpgradeWrapper;

@GameTestHolder(SophisticatedSmeltery.MODID)
@PrefixGameTestTemplate(false)
public final class SpawnerUpgradeGameTests {
    public static ItemStack spawner(BlockPos target, String type, int count) {
        ItemStack stack = new ItemStack(Items.SPAWNER);
        CompoundTag tag = new CompoundTag();
        tag.putShort("Delay", (short)0);
        tag.putShort("MinSpawnDelay", (short)40);
        tag.putShort("MaxSpawnDelay", (short)41);
        tag.putShort("SpawnCount", (short)count);
        tag.putShort("SpawnRange", (short)4);
        tag.putShort("MaxNearbyEntities", (short)6);
        tag.putShort("RequiredPlayerRange", (short)0);
        CompoundTag entity = new CompoundTag();
        entity.putString("id", type);
        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(target.getX() + 0.5));
        pos.add(DoubleTag.valueOf(target.getY()));
        pos.add(DoubleTag.valueOf(target.getZ() + 0.5));
        entity.put("Pos", pos);
        CompoundTag data = new CompoundTag();
        data.put("entity", entity);
        tag.put("SpawnData", data);
        tag.putString("test_preserved", "untouched");
        BlockItem.setBlockEntityData(stack, BlockEntityType.MOB_SPAWNER, tag);
        return stack;
    }
    public static BackpackWrapper backpack(boolean advanced, boolean catcher) {
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack((advanced ? SophisticatedSmeltery.ADVANCED_SPAWNER_UPGRADE : SophisticatedSmeltery.SPAWNER_UPGRADE).get()));
        if (catcher) storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
        return storage;
    }
    public static SpawnerUpgradeWrapper upgrade(IBackpackWrapper storage) {
        return (SpawnerUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
    }
    private static BlockPos target(GameTestHelper helper) { return helper.absolutePos(new BlockPos(1, 5, 1)); }
    private static List<Pig> pigs(GameTestHelper helper, BlockPos target) {
        return helper.getLevel().getEntitiesOfClass(Pig.class, new AABB(target).inflate(0.5));
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void recipesSlotsAndTierPreserveSpawnerAndSwitch(GameTestHelper helper) {
        var storage = backpack(false, false);
        var wrapper = upgrade(storage);
        ItemStack emptyDisabled = new ItemStack(SophisticatedSmeltery.SPAWNER_UPGRADE.get());
        emptyDisabled.getOrCreateTag().putBoolean("enabled", false);
        helper.assertTrue(new SpawnerUpgradeWrapper(storage, emptyDisabled, ignored -> {}).inventory().getSlots() == 1,
                "An empty upgrade with only settings NBT still has its spawner slot");
        helper.assertTrue(wrapper.inventory().getSlots() == 1 && wrapper.inventory().getSlotLimit(0) == 1
                && wrapper.getUpgradeStack().is(ModItems.BACKPACK_UPGRADE_TAG), "One real spawner slot and normal upgrade registration");
        helper.assertTrue(wrapper.inventory().insertItem(0, new ItemStack(Items.IRON_BARS), false).is(Items.IRON_BARS), "Reject non-spawners");
        wrapper.inventory().setStackInSlot(0, spawner(target(helper), "minecraft:pig", 3));
        wrapper.setEnabled(false);
        var menu = new AbstractContainerMenu(null, -1) {
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 3, 3);
        for (int i = 0; i < 9; i++) grid.setItem(i, new ItemStack(i == 4 ? ModItems.UPGRADE_BASE.get() : Items.IRON_BARS));
        var base = helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id("spawner_upgrade")).orElseThrow();
        helper.assertTrue(((net.minecraft.world.item.crafting.CraftingRecipe)base).matches(grid, helper.getLevel()), "Eight bars surround the upgrade base");
        grid.clearContent();
        grid.setItem(1, new ItemStack(Items.DIAMOND));
        grid.setItem(3, new ItemStack(Items.GOLD_INGOT));
        grid.setItem(4, wrapper.getUpgradeStack());
        grid.setItem(5, new ItemStack(Items.GOLD_INGOT));
        for (int i = 6; i < 9; i++) grid.setItem(i, new ItemStack(Items.REDSTONE));
        var tier = (net.minecraft.world.item.crafting.CraftingRecipe)helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id("advanced_spawner_upgrade")).orElseThrow();
        helper.assertTrue(tier.matches(grid, helper.getLevel()), "Standard advanced recipe");
        var result = tier.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(result.getTag().equals(wrapper.getUpgradeStack().getTag()), "Tier recipe copies spawner state and switch without refund duplication");
        result.getTag().putString("copy_probe", "changed");
        helper.assertTrue(!wrapper.getUpgradeStack().getTag().contains("copy_probe"), "Tier result has independent NBT");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void nativeRulesAdvancedExemptionsPauseAndRestore(GameTestHelper helper) {
        var pos = target(helper);
        var basic = upgrade(backpack(false, false));
        basic.inventory().setStackInSlot(0, spawner(pos, "minecraft:pig", 1));
        basic.tick(null, helper.getLevel(), pos.below(3));
        helper.assertTrue(pigs(helper, pos).isEmpty(), "Basic spawner requires a nearby player");
        var advanced = upgrade(backpack(true, false));
        advanced.inventory().setStackInSlot(0, spawner(pos, "minecraft:pig", 1));
        advanced.setEnabled(false);
        advanced.tick(null, helper.getLevel(), pos.below(3));
        helper.assertTrue(pigs(helper, pos).isEmpty(), "Disabled module does not spawn");
        advanced.setEnabled(true);
        advanced.tick(null, helper.getLevel(), pos.below(3));
        helper.assertTrue(pigs(helper, pos).size() == 1, "Advanced spawns without players, grass or natural conditions");
        var saved = BlockItem.getBlockEntityData(advanced.inventory().getStackInSlot(0));
        helper.assertTrue(saved.getShort("Delay") == 40 && saved.getString("test_preserved").equals("untouched"), "Native cooldown and unknown item NBT are saved");
        var restored = new SpawnerUpgradeWrapper(backpack(true, false), advanced.getUpgradeStack().copy(), ignored -> {});
        restored.setEnabled(false);
        restored.tick(null, helper.getLevel(), pos);
        helper.assertTrue(BlockItem.getBlockEntityData(restored.inventory().getStackInSlot(0)).getShort("Delay") == 40, "Pause survives wrapper reconstruction");
        restored.setEnabled(true);
        restored.tick(null, helper.getLevel(), pos);
        helper.assertTrue(BlockItem.getBlockEntityData(restored.inventory().getStackInSlot(0)).getShort("Delay") == 39, "Moving/reloading retains cooldown");
        helper.assertTrue(!saved.getBoolean("ignore_players") && !saved.getBoolean("ignore_conditions"), "Advanced permissions are never written to the spawner item");
        pigs(helper, pos).forEach(Pig::discard);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void capturesFullBatchAndFallsBackForFullOrIneligibleBackpacks(GameTestHelper helper) {
        var pos = target(helper);
        var storage = backpack(true, true);
        var wrapper = upgrade(storage);
        ItemStack fixedIdSpawner = spawner(pos, "minecraft:pig", 3);
        BlockItem.getBlockEntityData(fixedIdSpawner).getCompound("SpawnData").getCompound("entity")
                .putUUID("UUID", java.util.UUID.randomUUID());
        wrapper.inventory().setStackInSlot(0, fixedIdSpawner);
        wrapper.tick(null, helper.getLevel(), pos.below(3));
        helper.assertTrue(MobCatcherStorage.getCapturedMobs(storage).size() == 3 && pigs(helper, pos).isEmpty(), "Capture all three births without world entities");
        helper.assertTrue(BlockItem.getBlockEntityData(wrapper.inventory().getStackInSlot(0)).getShort("Delay") == 40, "Event capture retains native batch success and cooldown");
        helper.assertTrue(MobCatcherStorage.getCapturedMobs(storage).stream().map(mob -> mob.id()).distinct().count() == 3, "Each birth has a distinct identity");
        var full = backpack(true, true);
        for (int i = 0; i < full.getInventoryHandler().getSlots(); i++) full.getInventoryHandler().setStackInSlot(i, new ItemStack(Items.STONE, 64));
        upgrade(full).inventory().setStackInSlot(0, spawner(pos, "minecraft:pig", 1));
        upgrade(full).tick(null, helper.getLevel(), pos.below(3));
        helper.assertTrue(MobCatcherStorage.getCapturedMobs(full).isEmpty() && pigs(helper, pos).size() == 1, "Full inventory falls back to normal world spawning");
        pigs(helper, pos).forEach(Pig::discard);
        var hostile = backpack(true, true);
        upgrade(hostile).inventory().setStackInSlot(0, spawner(pos, "minecraft:zombie", 1));
        upgrade(hostile).tick(null, helper.getLevel(), pos.below(3));
        helper.assertTrue(MobCatcherStorage.getCapturedMobs(hostile).isEmpty(), "Basic catcher never accepts hostile births");
        var zombies = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, new AABB(pos).inflate(1));
        helper.assertTrue(zombies.size() == 1, "Ineligible hostile births fall back to normal world spawning");
        zombies.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID, timeoutTicks = 100)
    public static void actualCarriedAndPlacedBackpacksRunSpawner(GameTestHelper helper) {
        var pos = target(helper);
        var placed = backpack(true, true);
        upgrade(placed).inventory().setStackInSlot(0, spawner(pos, "minecraft:pig", 1));
        helper.getLevel().setBlockAndUpdate(pos.below(3), ModBlocks.NETHERITE_BACKPACK.get().defaultBlockState());
        var block = (BackpackBlockEntity)helper.getLevel().getBlockEntity(pos.below(3));
        block.setBackpack(placed.getBackpack());
        var carried = backpack(true, true);
        upgrade(carried).inventory().setStackInSlot(0, spawner(pos.east(2), "minecraft:pig", 1));
        var player = helper.makeMockPlayer();
        ItemStack held = carried.getBackpack();
        helper.onEachTick(() -> ((BackpackItem)held.getItem()).inventoryTick(held, helper.getLevel(), player, -1, false));
        helper.succeedWhen(() -> {
            helper.assertTrue(!MobCatcherStorage.getCapturedMobs(block.getBackpackWrapper()).isEmpty(), "Placed host ticks the installed native spawner");
            helper.assertTrue(!MobCatcherStorage.getCapturedMobs(carried).isEmpty(), "Carried host ticks the installed native spawner");
            helper.getLevel().removeBlock(pos.below(3), false);
        });
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void adjacentWorldSpawnerCapturesWithoutSpawnerUpgrade(GameTestHelper helper) {
        var pos = target(helper).below(3);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.SPAWNER.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.east(), ModBlocks.NETHERITE_BACKPACK.get().defaultBlockState());
        var block = (BackpackBlockEntity)helper.getLevel().getBlockEntity(pos.east());
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
        block.setBackpack(storage.getBackpack());
        var tile = (SpawnerBlockEntity)helper.getLevel().getBlockEntity(pos);
        var data = BlockItem.getBlockEntityData(spawner(pos.above(3), "minecraft:pig", 2)).copy();
        data.putShort("RequiredPlayerRange", (short)16);
        // Native custom light rules permit testing pig births on the empty test structure.
        CompoundTag light = new CompoundTag();
        // Use explicit unrestricted ranges, just as a data-pack spawner can.
        CompoundTag range = new CompoundTag(); range.putInt("min_inclusive", 0); range.putInt("max_inclusive", 15);
        light.put("block_light_limit", range.copy()); light.put("sky_light_limit", range.copy());
        data.getCompound("SpawnData").put("custom_spawn_rules", light);
        tile.load(data);
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "spawner-test"));
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().players().add(player);
        try { SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile); }
        finally { helper.getLevel().players().remove(player); }
        helper.assertTrue(MobCatcherStorage.getCapturedMobs(block.getBackpackWrapper()).size() == 2, "Adjacent world spawner captures its full batch with only a catcher upgrade");
        helper.assertTrue(pigs(helper, pos.above(3)).isEmpty(), "Captured births never enter the world");
        // Natural/non-spawner additions must not be intercepted just because a backpack is nearby.
        Pig pig = EntityType.PIG.create(helper.getLevel());
        pig.moveTo(pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5);
        helper.assertTrue(helper.getLevel().addFreshEntity(pig) && !pig.isRemoved(), "Ordinary entity additions are untouched");
        pig.discard();
        helper.getLevel().removeBlock(pos, false); helper.getLevel().removeBlock(pos.east(), false);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void adjacentAdvancedUpgradeAppliesOnSixFacesAndWithdrawsImmediately(GameTestHelper helper) {
        var pos = target(helper).below(3);
        var birth = pos.above(3);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.SPAWNER.defaultBlockState());
        var tile = (SpawnerBlockEntity)helper.getLevel().getBlockEntity(pos);
        var data = BlockItem.getBlockEntityData(spawner(birth, "minecraft:pig", 1)).copy();
        tile.load(data);
        SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
        helper.assertTrue(pigs(helper, birth).isEmpty(), "Unaided world spawner keeps its native player requirement");

        for (Direction side : Direction.values()) {
            var adjacent = pos.relative(side);
            helper.getLevel().setBlockAndUpdate(adjacent, ModBlocks.NETHERITE_BACKPACK.get().defaultBlockState());
            var block = (BackpackBlockEntity)helper.getLevel().getBlockEntity(adjacent);
            block.setBackpack(backpack(true, false).getBackpack());
            var upgrade = upgrade(block.getBackpackWrapper());
            helper.assertTrue(upgrade.inventory().getStackInSlot(0).isEmpty(), "Support does not require an internal spawner");
            tile.load(data);
            SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
            helper.assertTrue(pigs(helper, birth).size() == 1, "Empty advanced upgrade supports face " + side);
            pigs(helper, birth).forEach(Pig::discard);
            var saved = tile.saveWithoutMetadata();
            helper.assertTrue(saved.getShort("Delay") == 40 && !saved.getBoolean("ignore_players")
                    && !saved.getBoolean("ignore_conditions") && !saved.getCompound("SpawnData").contains("custom_spawn_rules"),
                    "World spawner keeps its native delay and never saves the temporary permissions");

            upgrade.setEnabled(false);
            tile.load(data);
            SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
            helper.assertTrue(pigs(helper, birth).isEmpty() && tile.saveWithoutMetadata().getShort("Delay") == 0,
                    "Disabling the adjacent upgrade immediately restores player gating");
            upgrade.setEnabled(true);
            SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
            helper.assertTrue(pigs(helper, birth).size() == 1, "Re-enabling immediately restores support");
            pigs(helper, birth).forEach(Pig::discard);

            block.getBackpackWrapper().getUpgradeHandler().setStackInSlot(0, ItemStack.EMPTY);
            tile.load(data);
            SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
            helper.assertTrue(pigs(helper, birth).isEmpty(), "Removing the upgrade immediately withdraws support");
            block.setBackpack(backpack(true, false).getBackpack());
            helper.getLevel().removeBlock(adjacent, false);
            SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
            helper.assertTrue(pigs(helper, birth).isEmpty(), "Removing the backpack immediately withdraws support");
        }

        helper.getLevel().setBlockAndUpdate(pos.east(), ModBlocks.NETHERITE_BACKPACK.get().defaultBlockState());
        ((BackpackBlockEntity)helper.getLevel().getBlockEntity(pos.east())).setBackpack(backpack(false, false).getBackpack());
        SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
        helper.assertTrue(pigs(helper, birth).isEmpty(), "A basic upgrade does not grant adjacent exemptions");
        helper.getLevel().removeBlock(pos.east(), false);
        var diagonal = pos.east().north();
        helper.getLevel().setBlockAndUpdate(diagonal, ModBlocks.NETHERITE_BACKPACK.get().defaultBlockState());
        ((BackpackBlockEntity)helper.getLevel().getBlockEntity(diagonal)).setBackpack(backpack(true, false).getBackpack());
        SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
        helper.assertTrue(pigs(helper, birth).isEmpty(), "Diagonal backpacks do not grant exemptions");
        helper.getLevel().removeBlock(diagonal, false);

        // Withdrawal restores normal operation; it does not disable the external spawner.
        var nativeData = data.copy();
        nativeData.putShort("RequiredPlayerRange", (short)16);
        CompoundTag light = new CompoundTag();
        CompoundTag range = new CompoundTag(); range.putInt("min_inclusive", 0); range.putInt("max_inclusive", 15);
        light.put("block_light_limit", range.copy()); light.put("sky_light_limit", range.copy());
        nativeData.getCompound("SpawnData").put("custom_spawn_rules", light);
        tile.load(nativeData);
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "adjacent-spawner"));
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().players().add(player);
        try { SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile); }
        finally { helper.getLevel().players().remove(player); }
        helper.assertTrue(pigs(helper, birth).size() == 1, "External spawner still runs when its own conditions are met");
        pigs(helper, birth).forEach(Pig::discard);
        helper.getLevel().removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void adjacentSupportKeepsCaptureLimitsAndApotheosisModifiers(GameTestHelper helper) {
        var pos = target(helper).below(3);
        var birth = pos.above(3);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.SPAWNER.defaultBlockState());
        var tile = (SpawnerBlockEntity)helper.getLevel().getBlockEntity(pos);
        helper.getLevel().setBlockAndUpdate(pos.north(), ModBlocks.NETHERITE_BACKPACK.get().defaultBlockState());
        var support = (BackpackBlockEntity)helper.getLevel().getBlockEntity(pos.north());
        support.setBackpack(backpack(true, false).getBackpack());
        helper.getLevel().setBlockAndUpdate(pos.south(), ModBlocks.NETHERITE_BACKPACK.get().defaultBlockState());
        var catcher = (BackpackBlockEntity)helper.getLevel().getBlockEntity(pos.south());
        var storage = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
        catcher.setBackpack(storage.getBackpack());
        var actual = catcher.getBackpackWrapper();
        var data = BlockItem.getBlockEntityData(spawner(birth, "minecraft:pig", 2)).copy();
        tile.load(data);
        SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
        helper.assertTrue(MobCatcherStorage.getCapturedMobs(actual).size() == 2 && pigs(helper, birth).isEmpty(),
                "Advanced support and capture can come from different adjacent backpacks, preserving the full batch");
        helper.assertTrue(tile.saveWithoutMetadata().getShort("Delay") == 40, "Capture keeps native cooldown");

        tile.load(data);
        helper.getLevel().setBlockAndUpdate(birth, Blocks.STONE.defaultBlockState());
        SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
        helper.assertTrue(MobCatcherStorage.getCapturedMobs(actual).size() == 2, "Adjacent support still checks collision");
        helper.getLevel().removeBlock(birth, false);
        var limited = data.copy(); limited.putShort("MaxNearbyEntities", (short)0);
        tile.load(limited);
        SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
        helper.assertTrue(MobCatcherStorage.getCapturedMobs(actual).size() == 2, "Adjacent support still checks nearby limits");

        var full = new BackpackWrapper(new ItemStack(ModItems.NETHERITE_BACKPACK.get()));
        full.getInventoryHandler();
        full.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
        for (int i = 0; i < full.getInventoryHandler().getSlots(); i++) full.getInventoryHandler().setStackInSlot(i, new ItemStack(Items.STONE, 64));
        catcher.setBackpack(full.getBackpack());
        var single = BlockItem.getBlockEntityData(spawner(birth, "minecraft:pig", 1)).copy();
        tile.load(single);
        SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
        helper.assertTrue(pigs(helper, birth).size() == 1 && MobCatcherStorage.getCapturedMobs(catcher.getBackpackWrapper()).isEmpty(),
                "A full catcher falls back to world spawning while adjacent exemptions remain active");
        pigs(helper, birth).forEach(Pig::discard);

        if (ModList.get().isLoaded("apotheosis")) {
            catcher.setBackpack(storage.getBackpack());
            int before = MobCatcherStorage.getCapturedMobs(catcher.getBackpackWrapper()).size();
            var modified = single.copy();
            modified.putBoolean("redstone_control", true);
            modified.putBoolean("no_ai", true); modified.putBoolean("silent", true); modified.putBoolean("baby", true);
            tile.load(modified);
            SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
            helper.assertTrue(MobCatcherStorage.getCapturedMobs(catcher.getBackpackWrapper()).size() == before,
                    "Adjacent upgrade preserves Apotheosis redstone gating");
            helper.getLevel().setBlockAndUpdate(pos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            SpawnerBlockEntity.serverTick(helper.getLevel(), pos, tile.getBlockState(), tile);
            var mobs = MobCatcherStorage.getCapturedMobs(catcher.getBackpackWrapper());
            helper.assertTrue(mobs.size() == before + 1, "Real Apotheosis world spawner captures once powered");
            var mob = mobs.get(mobs.size() - 1).entityNbt();
            helper.assertTrue(mob.getBoolean("NoAI") && mob.getBoolean("Silent") && mob.getInt("Age") < 0,
                    "Adjacent support retains native Apotheosis entity modifiers");
            helper.getLevel().removeBlock(pos.west(), false);
        }
        helper.getLevel().removeBlock(pos.north(), false);
        helper.getLevel().removeBlock(pos.south(), false);
        helper.getLevel().removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void advancedStillChecksCollisionAndNearbyLimits(GameTestHelper helper) {
        var pos = target(helper);
        var wrapper = upgrade(backpack(true, false));
        wrapper.inventory().setStackInSlot(0, spawner(pos, "minecraft:pig", 1));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        wrapper.tick(null, helper.getLevel(), pos.below(3));
        helper.assertTrue(pigs(helper, pos).isEmpty(), "Advanced does not generate inside solid blocks");
        helper.getLevel().removeBlock(pos, false);
        ItemStack limited = spawner(pos, "minecraft:pig", 1);
        var tag = BlockItem.getBlockEntityData(limited); tag.putShort("MaxNearbyEntities", (short)0);
        wrapper.inventory().setStackInSlot(0, limited);
        wrapper.tick(null, helper.getLevel(), pos.below(3));
        helper.assertTrue(pigs(helper, pos).isEmpty(), "Advanced still respects the native nearby limit");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void apotheosisFactoryModifiersAndRedstoneArePreserved(GameTestHelper helper) {
        if (!ModList.get().isLoaded("apotheosis")) { helper.succeed(); return; }
        var pos = target(helper);
        var storage = backpack(true, true);
        var wrapper = upgrade(storage);
        ItemStack stack = spawner(pos, "minecraft:pig", 1);
        var tag = BlockItem.getBlockEntityData(stack);
        tag.putBoolean("redstone_control", true);
        tag.putBoolean("no_ai", true);
        tag.putBoolean("silent", true);
        tag.putBoolean("baby", true);
        wrapper.inventory().setStackInSlot(0, stack);
        wrapper.tick(null, helper.getLevel(), pos.below(3));
        helper.assertTrue(wrapper.nativeSpawner().getClass().getName().endsWith("ApothSpawnerTile"), "Use actual registered Apotheosis tile");
        helper.assertTrue(MobCatcherStorage.getCapturedMobs(storage).isEmpty(), "Advanced preserves native redstone gating");
        helper.getLevel().setBlockAndUpdate(pos.below(3).west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        wrapper.tick(null, helper.getLevel(), pos.below(3));
        var mobs = MobCatcherStorage.getCapturedMobs(storage);
        helper.assertTrue(mobs.size() == 1, "Actual Apotheosis spawner captures after redstone activation");
        var mob = mobs.get(0).entityNbt();
        helper.assertTrue(mob.getBoolean("NoAI") && mob.getBoolean("Silent") && mob.getInt("Age") < 0, "Keep no-AI, silent and baby modifiers on captured births");
        helper.assertTrue(BlockItem.getBlockEntityData(wrapper.inventory().getStackInSlot(0)).getBoolean("redstone_control"), "Modified spawner NBT survives persistence");
        helper.getLevel().removeBlock(pos.below(3).west(), false);
        helper.succeed();
    }
}
