package org.brahypno.sophisticated_smeltery;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkedStorageEndpointAdapter;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.brahypno.sophisticated_smeltery.compat.StorageCompat;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

@GameTestHolder(SophisticatedSmeltery.MODID)
@PrefixGameTestTemplate(false)
public final class StorageCompatibilityGameTests {
    private static StorageBlockEntity place(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, block);
        return (StorageBlockEntity)helper.getBlockEntity(pos);
    }
    static MeltingUpgradeWrapper install(IStorageWrapper storage) {
        var stack = ControllerBinding.bind(new ItemStack(storage.getStorageType().equals("backpack") ? SophisticatedSmeltery.MELTING_UPGRADE.get() : SophisticatedSmeltery.STORAGE_MELTING_UPGRADE.get()),
                new ItemStack(TinkerSmeltery.smelteryController));
        if (!storage.getUpgradeHandler().insertItem(0, stack, false).isEmpty())
            throw new IllegalStateException("Storage rejected the controller upgrade");
        var furnace = furnace(storage);
        furnace.getComponents().setStackInSlot(0, new ItemStack(TinkerSmeltery.searedBricks, 32));
        furnace.setHeating(false);
        return furnace;
    }
    private static MeltingUpgradeWrapper furnace(IStorageWrapper storage) {
        return (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
    }
    private static FluidStack iron(int amount) { return new FluidStack(TinkerFluids.moltenIron.get(), amount); }
    private static IFluidHandler fluid(StorageBlockEntity block) {
        return block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElseThrow(IllegalStateException::new);
    }
    private record Link(StorageBlockEntity primary, StorageBlockEntity secondary, LinkedStorageGroupManager manager,
                        LinkedStorageEndpointData first, LinkedStorageEndpointData second) {}
    private static Link linked(GameTestHelper helper, int amount) {
        var primary = place(helper, new BlockPos(1, 1, 1), ModBlocks.NETHERITE_CHEST.get());
        var secondary = place(helper, new BlockPos(3, 1, 1), ModBlocks.NETHERITE_CHEST.get());
        var furnace = install(primary.getStorageWrapper());
        furnace.getTank().fill(iron(amount), FluidAction.EXECUTE);
        furnace.persist();
        var adapter = new StorageLinkedStorageEndpointAdapter();
        var manager = LinkedStorageGroupsSavedData.get(helper.getLevel()).manager();
        var endpoint = UUID.randomUUID();
        var group = manager.createGroup(UUID.randomUUID(), endpoint,
                adapter.createHostDescriptor(helper.getLevel(), primary), adapter.copyCanonicalContents(helper.getLevel(), primary));
        var first = new LinkedStorageEndpointData(group, endpoint);
        adapter.bindEndpoint(helper.getLevel(), primary, first);
        var second = manager.createSecondaryEndpoint(first).orElseThrow();
        adapter.bindEndpoint(helper.getLevel(), secondary, second);
        return new Link(primary, secondary, manager, first, second);
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void supportedContainersExposeAndPersistFluids(GameTestHelper helper) {
        List<Block> blocks = List.of(ModBlocks.NETHERITE_CHEST.get(), ModBlocks.NETHERITE_BARREL.get(),
                ModBlocks.NETHERITE_SHULKER_BOX.get(), ModBlocks.LIMITED_NETHERITE_BARREL_1.get(),
                ModBlocks.LIMITED_NETHERITE_BARREL_2.get(), ModBlocks.LIMITED_NETHERITE_BARREL_3.get(),
                ModBlocks.LIMITED_NETHERITE_BARREL_4.get());
        for (int index = 0; index < blocks.size(); index++) {
            var block = place(helper, new BlockPos(1 + index % 3, 1, 1 + index / 3), blocks.get(index));
            var furnace = install(block.getStorageWrapper());
            var handler = fluid(block);
            var before = furnace.getUpgradeStack().copy();
            helper.assertTrue(handler.fill(iron(1000), FluidAction.SIMULATE) == 1000
                    && ItemStack.matches(before, furnace.getUpgradeStack()), "Simulation must not save or mutate " + blocks.get(index));
            helper.assertTrue(handler.fill(iron(1000), FluidAction.EXECUTE) == 1000, "Pipe input on " + blocks.get(index));
            helper.assertTrue(handler.drain(iron(300), FluidAction.EXECUTE).getAmount() == 300, "Pipe output on " + blocks.get(index));
            var reload = new MeltingUpgradeWrapper(block.getStorageWrapper(), furnace.getUpgradeStack().copy(), ignored -> {});
            helper.assertTrue(reload.getTank().getContained() == 700, "Pipe transfers persist immediately on " + blocks.get(index));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void manualBucketsCastingAndAlloyerRemainAvailable(GameTestHelper helper) {
        var block = place(helper, new BlockPos(1, 1, 1), ModBlocks.NETHERITE_CHEST.get());
        var storage = block.getStorageWrapper();
        install(storage);
        storage.getUpgradeHandler().insertItem(1, new ItemStack(SophisticatedSmeltery.STORAGE_SEARED_CASTING_UPGRADE.get()), false);
        storage.getUpgradeHandler().insertItem(2, new ItemStack(SophisticatedSmeltery.STORAGE_ALLOYER_UPGRADE.get()), false);
        storage.getUpgradeHandler().insertItem(3, new ItemStack(SophisticatedSmeltery.STORAGE_SPAWNER_UPGRADE.get()), false);
        var casting = (CastingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(1);
        var alloyer = (AlloyerUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(2);
        helper.assertTrue(storage.getUpgradeHandler().getSlotWrappers().size() == 4, "All upgrade families install through the real handler");
        var furnace = furnace(storage);
        furnace.getContainers().setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));
        furnace.transferContainer();
        helper.assertTrue(furnace.getTank().getContained() == 1000 && furnace.getContainers().getStackInSlot(1).is(Items.BUCKET),
                "Manual bucket insertion uses the furnace tank without backpacks");
        furnace.getTank().drain(1000, FluidAction.EXECUTE);
        furnace.getTank().fill(iron(90), FluidAction.EXECUTE);
        casting.casting().inventory().setStackInSlot(0, new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("tconstruct", "ingot_cast"))));
        helper.assertTrue(casting.pour(helper.getLevel()) == 90, "Manual casting uses the installed furnace fluid");
        for (int tick = 0; tick < 80; tick++) casting.tick(null, helper.getLevel(), block.getBlockPos());
        helper.assertTrue(!casting.casting().inventory().getStackInSlot(1).isEmpty(), "Casting completes in a storage container");
        helper.assertTrue(alloyer.getBackpackTanks().isEmpty(), "Storage does not invent a tank upgrade");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void capabilityInvalidatesAndRespectsLockPackingAndUnload(GameTestHelper helper) {
        var block = place(helper, new BlockPos(1, 1, 1), ModBlocks.NETHERITE_CHEST.get());
        install(block.getStorageWrapper());
        var first = block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH);
        fluid(block).fill(iron(100), FluidAction.EXECUTE);
        block.toggleLock();
        helper.assertTrue(fluid(block).fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE) == 0,
                "Locked storage rejects fluid types absent from its contents");
        helper.assertTrue(fluid(block).fill(iron(100), FluidAction.EXECUTE) == 100, "Locked storage accepts existing fluid");
        block.toggleLock();
        block.getStorageWrapper().getUpgradeHandler().setStackInSlot(0, ItemStack.EMPTY);
        helper.assertTrue(!first.isPresent() && !block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).isPresent(),
                "Removing the fluid upgrade invalidates the previous capability");
        install(block.getStorageWrapper());
        var beforePacking = block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH);
        ((WoodStorageBlockEntity)block).setPacked(true);
        helper.assertTrue(!beforePacking.isPresent() && !block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).isPresent(),
                "Packing invalidates existing fluid access and blocks new access");
        ((WoodStorageBlockEntity)block).setPacked(false);
        var beforeUnload = block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH);
        block.onChunkUnloaded();
        helper.assertTrue(!beforeUnload.isPresent() && !block.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).isPresent(),
                "Unloaded containers cannot reacquire fluid capability");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void linkedEndpointsShareOneTankAndDetachInvalidatesCapability(GameTestHelper helper) {
        var link = linked(helper, 1000);
        helper.assertTrue(link.primary.getStorageWrapper().getFluidHandler().orElseThrow()
                        == link.secondary.getStorageWrapper().getFluidHandler().orElseThrow(), "Endpoints share the canonical handler instance");
        helper.assertTrue(fluid(link.secondary).fill(iron(500), FluidAction.EXECUTE) == 500, "Secondary pipe input");
        helper.assertTrue(fluid(link.primary).drain(iron(700), FluidAction.EXECUTE).getAmount() == 700, "Primary pipe output");
        helper.assertTrue(furnace(link.primary.getStorageWrapper()).getTank().getContained() == 800
                        && furnace(link.secondary.getStorageWrapper()).getTank().getContained() == 800, "Fluid is conserved across endpoints");
        var secondaryCap = link.secondary.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH);
        helper.assertTrue(link.manager.detachLostEndpoint(link.second.groupId(), link.second.endpointId()), "Detach secondary through native manager");
        helper.assertTrue(!secondaryCap.isPresent() && !link.secondary.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).isPresent(),
                "A detached endpoint loses its cached and future fluid access");
        helper.assertTrue(fluid(link.primary).drain(1000, FluidAction.EXECUTE).getAmount() == 800, "Detached endpoint neither duplicates nor loses fluid");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void linkedRootReplacementAndPrimaryRebindKeepCanonicalFluids(GameTestHelper helper) {
        var link = linked(helper, 600);
        var oldPrimary = link.primary.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH);
        var oldSecondary = link.secondary.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH);
        var contents = link.manager.resolveContents(link.first.groupId()).orElseThrow();
        CompoundTag snapshot = contents.getContents().copy();
        fluid(link.secondary).fill(iron(200), FluidAction.EXECUTE);
        contents.setContents(snapshot);
        helper.assertTrue(!oldPrimary.isPresent() && !oldSecondary.isPresent(), "Root replacement invalidates all endpoint capabilities");
        helper.assertTrue(furnace(link.primary.getStorageWrapper()).getTank().getContained() == 600, "Root reload restores exactly the saved liquid amount");
        var primary = link.primary;
        CompoundTag saved = primary.saveWithFullMetadata();
        primary.onChunkUnloaded();
        helper.getLevel().removeBlockEntity(primary.getBlockPos());
        var restored = new net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity(primary.getBlockPos(), primary.getBlockState());
        restored.load(saved);
        helper.getLevel().setBlockEntity(restored);
        restored.onLoad();
        helper.assertTrue(restored.getStorageWrapper().getFluidHandler().orElseThrow()
                        == link.secondary.getStorageWrapper().getFluidHandler().orElseThrow(), "Rebound primary resolves the same canonical handler");
        helper.assertTrue(fluid(restored).drain(1000, FluidAction.EXECUTE).getAmount() == 600, "Primary reload conserves fluid");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void linkedUpgradesRunOnlyAtPrimaryEndpoint(GameTestHelper helper) {
        var link = linked(helper, 180);
        var storage = link.primary.getStorageWrapper();
        storage.getUpgradeHandler().insertItem(1, new ItemStack(SophisticatedSmeltery.STORAGE_ADVANCED_SEARED_CASTING_UPGRADE.get()), false);
        var casting = (CastingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(1);
        casting.casting().inventory().setStackInSlot(0, new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("tconstruct", "ingot_cast"))));
        casting.setAutomatic(true);
        StorageBlockEntity.serverTick(helper.getLevel(), link.secondary.getBlockPos(), link.secondary);
        helper.assertTrue(furnace(storage).getTank().getContained() == 180, "Secondary does not pour or run shared upgrades");
        StorageBlockEntity.serverTick(helper.getLevel(), link.primary.getBlockPos(), link.primary);
        helper.assertTrue(furnace(storage).getTank().getContained() == 90 && casting.casting().isCooling(), "Primary runs one shared casting upgrade");
        int timer = casting.casting().timer();
        for (int tick = 0; tick < 10; tick++) StorageBlockEntity.serverTick(helper.getLevel(), link.secondary.getBlockPos(), link.secondary);
        helper.assertTrue(casting.casting().timer() == timer, "Additional endpoints cannot accelerate cooling");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void storageSpawnerProducesWorldEntitiesWithoutCatcher(GameTestHelper helper) {
        var block = place(helper, new BlockPos(1, 1, 1), ModBlocks.NETHERITE_CHEST.get());
        var storage = block.getStorageWrapper();
        storage.getUpgradeHandler().insertItem(0, new ItemStack(SophisticatedSmeltery.STORAGE_SPAWNER_UPGRADE.get()), false);
        storage.getUpgradeHandler().insertItem(1, new ItemStack(SophisticatedSmeltery.STORAGE_ADVANCED_SPAWNER_UPGRADE.get()), false);
        var basic = (org.brahypno.sophisticated_smeltery.upgrade.SpawnerUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
        var advanced = (org.brahypno.sophisticated_smeltery.upgrade.SpawnerUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(1);
        BlockPos target = helper.absolutePos(new BlockPos(1, 5, 1));
        var entity = new CompoundTag();
        entity.putString("id", "minecraft:pig");
        var position = new net.minecraft.nbt.ListTag();
        position.add(net.minecraft.nbt.DoubleTag.valueOf(target.getX() + 0.5));
        position.add(net.minecraft.nbt.DoubleTag.valueOf(target.getY()));
        position.add(net.minecraft.nbt.DoubleTag.valueOf(target.getZ() + 0.5));
        entity.put("Pos", position);
        var data = new CompoundTag();
        data.put("entity", entity);
        var state = new CompoundTag();
        state.put("SpawnData", data);
        state.putShort("Delay", (short)0);
        state.putShort("MinSpawnDelay", (short)40);
        state.putShort("MaxSpawnDelay", (short)41);
        state.putShort("SpawnCount", (short)1);
        state.putShort("RequiredPlayerRange", (short)0);
        var spawner = new ItemStack(Items.SPAWNER);
        net.minecraft.world.item.BlockItem.setBlockEntityData(spawner, net.minecraft.world.level.block.entity.BlockEntityType.MOB_SPAWNER, state);
        basic.inventory().setStackInSlot(0, spawner.copy());
        advanced.inventory().setStackInSlot(0, spawner.copy());
        var bounds = new net.minecraft.world.phys.AABB(target).inflate(0.5);
        basic.tick(null, helper.getLevel(), block.getBlockPos());
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, bounds).isEmpty(), "Basic storage spawner retains the nearby-player requirement");
        advanced.setEnabled(false);
        advanced.tick(null, helper.getLevel(), block.getBlockPos());
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, bounds).isEmpty(), "Disabled storage spawner does not spawn");
        advanced.setEnabled(true);
        advanced.tick(null, helper.getLevel(), block.getBlockPos());
        var spawned = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, bounds);
        helper.assertTrue(spawned.size() == 1, "Storage spawner produces a real world entity without a catcher");
        for (int slot = 0; slot < storage.getInventoryHandler().getSlots(); slot++)
            helper.assertTrue(storage.getInventoryHandler().getStackInSlot(slot).isEmpty(), "World births are not stored in container inventory");
        advanced.setEnabled(false);
        spawned.forEach(net.minecraft.world.entity.Entity::discard);
        if (!ModList.get().isLoaded("sophisticatedbackpacks")) {
            var worldPos = target.below(2);
            helper.getLevel().setBlockAndUpdate(worldPos, net.minecraft.world.level.block.Blocks.SPAWNER.defaultBlockState());
            var tile = (net.minecraft.world.level.block.entity.SpawnerBlockEntity)helper.getLevel().getBlockEntity(worldPos);
            tile.load(state);
            net.minecraft.world.level.block.entity.SpawnerBlockEntity.serverTick(helper.getLevel(), worldPos, tile.getBlockState(), tile);
            helper.assertTrue(tile.saveWithoutMetadata().getShort("Delay") == 0,
                    "World spawner entry keeps native gating and never loads optional backpack classes when backpacks is absent");
            helper.getLevel().removeBlock(worldPos, false);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void hostFamiliesRegisterAndLoadOnlyTheirOwnRecipes(GameTestHelper helper) {
        for (String prefix : List.of("", "storage_")) {
            String host = prefix.isEmpty() ? "sophisticatedbackpacks" : "sophisticatedstorage";
            boolean loaded = ModList.get().isLoaded(host);
            for (String item : List.of("melting_upgrade", "scorched_alloyer_upgrade", "spawner_upgrade", "advanced_spawner_upgrade",
                    "advanced_transmute_upgrade", "seared_casting_upgrade", "advanced_seared_casting_upgrade", "scorched_casting_upgrade", "advanced_scorched_casting_upgrade"))
                helper.assertTrue(ForgeRegistries.ITEMS.containsKey(SophisticatedSmeltery.id(prefix + item)) == (loaded && (!item.equals("advanced_transmute_upgrade") || ModList.get().isLoaded("esotericism_tinker"))), "Conditional item registration: " + prefix + item);
            for (String name : List.of("melting_upgrade", "alloyer_upgrade", "spawner_upgrade", "advanced_spawner_upgrade", "advanced_transmute_upgrade",
                    "seared_casting_upgrade", "advanced_seared_casting_upgrade", "scorched_casting_upgrade", "advanced_scorched_casting_upgrade",
                    "smeltery_upgrade", "foundry_upgrade", "transmute_upgrade")) {
                boolean expected = loaded && (!name.contains("transmute") || ModList.get().isLoaded("esotericism_tinker"));
                var id = SophisticatedSmeltery.id(prefix + name);
                var recipe = helper.getLevel().getRecipeManager().byKey(id);
                helper.assertTrue(recipe.isPresent() == expected, "Conditional recipe: " + id);
                helper.assertTrue((helper.getLevel().getServer().getAdvancements().getAdvancement(SophisticatedSmeltery.id((name.contains("spawner") ? "recipes/misc/" : "recipes/") + prefix + name)) != null) == expected, "Conditional unlock advancement: " + id);
                if (expected) verifyRecipe(helper, recipe.orElseThrow(), prefix);
            }
            if (loaded) {
                var recipe = helper.getLevel().getRecipeManager().byKey(SophisticatedSmeltery.id(prefix + "melting_upgrade")).orElseThrow();
                var base = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(host, "upgrade_base")));
                helper.assertTrue(recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(base)), "Recipe uses the correct host plate");
                String other = prefix.isEmpty() ? "sophisticatedstorage" : "sophisticatedbackpacks";
                var otherBase = ForgeRegistries.ITEMS.getValue(new ResourceLocation(other, "upgrade_base"));
                if (otherBase != null && otherBase != Items.AIR)
                    helper.assertTrue(recipe.getIngredients().stream().noneMatch(ingredient -> ingredient.test(new ItemStack(otherBase))), "Host plates cannot craft the opposite upgrade family");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SophisticatedSmeltery.MODID)
    public static void hostConversionRecipesPreserveUpgradeState(GameTestHelper helper) {
        var menu = new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        };
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 3, 3);
        for (String name : List.of("melting_upgrade", "scorched_alloyer_upgrade", "spawner_upgrade", "advanced_spawner_upgrade",
                "advanced_transmute_upgrade", "seared_casting_upgrade", "advanced_seared_casting_upgrade", "scorched_casting_upgrade", "advanced_scorched_casting_upgrade")) {
            boolean expected = ModList.get().isLoaded("sophisticatedbackpacks") && ModList.get().isLoaded("sophisticatedstorage")
                    && (!name.contains("transmute") || ModList.get().isLoaded("esotericism_tinker"));
            for (boolean toStorage : List.of(true, false)) {
                String recipeName = toStorage ? "storage_" + name + "_from_backpack_" + name : "backpack_" + name + "_from_storage_" + name;
                var id = SophisticatedSmeltery.id(recipeName);
                var loaded = helper.getLevel().getRecipeManager().byKey(id);
                helper.assertTrue(loaded.isPresent() == expected, "Conversion requires both hosts and its optional integration: " + id);
                helper.assertTrue((helper.getLevel().getServer().getAdvancements().getAdvancement(SophisticatedSmeltery.id("recipes/misc/" + recipeName)) != null) == expected,
                        "Conversion unlock uses the same conditions: " + id);
                if (!expected) continue;
                var source = ForgeRegistries.ITEMS.getValue(SophisticatedSmeltery.id((toStorage ? "" : "storage_") + name));
                var target = ForgeRegistries.ITEMS.getValue(SophisticatedSmeltery.id((toStorage ? "storage_" : "") + name));
                ItemStack input = new ItemStack(source);
                input.setHoverName(net.minecraft.network.chat.Component.literal("Configured upgrade"));
                // Real serialized controller and inventory data must survive changing the upgrade plate.
                if (name.equals("melting_upgrade")) input = ControllerBinding.bind(input, new ItemStack(TinkerSmeltery.foundryController));
                var state = new CompoundTag();
                state.put("storedItem", new ItemStack(Items.DIAMOND, 7).save(new CompoundTag()));
                input.getOrCreateTag().put("conversionState", state);
                var original = input.copy();
                grid.clearContent();
                grid.setItem(1, input);
                for (int slot : List.of(0, 2, 6, 8)) grid.setItem(slot, new ItemStack(toStorage ? Items.OAK_PLANKS : Items.STRING));
                grid.setItem(4, new ItemStack(toStorage ? Items.OAK_PLANKS : Items.LEATHER));
                var recipe = helper.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, helper.getLevel()).orElseThrow();
                helper.assertTrue(recipe.getId().equals(id), "Native conversion layout resolves the intended recipe: " + id);
                var result = recipe.assemble(grid, helper.getLevel().registryAccess());
                helper.assertTrue(result.is(target) && result.getCount() == 1 && original.getTag().equals(result.getTag()),
                        "Conversion preserves tier, controller, inventory and settings: " + id);
                helper.assertTrue(ItemStack.matches(input, original), "Crafting preview leaves the source untouched");
                result.getTag().getCompound("conversionState").putInt("changed", 1);
                helper.assertTrue(ItemStack.matches(input, original), "Converted state is an independent copy");
                for (var remaining : recipe.getRemainingItems(grid)) helper.assertTrue(remaining.isEmpty(), "Conversion does not duplicate stored items or controllers");
                var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try {
                    var serializer = new org.brahypno.sophisticated_smeltery.recipe.CastingTierRecipe.Serializer();
                    serializer.toNetwork(buffer, (org.brahypno.sophisticated_smeltery.recipe.CastingTierRecipe)recipe);
                    var synced = serializer.fromNetwork(id, buffer);
                    helper.assertTrue(synced.matches(grid, helper.getLevel()) && synced.assemble(grid, helper.getLevel().registryAccess()).is(target),
                            "Client recipe synchronization retains conversion inputs and output");
                } finally { buffer.release(); }
                grid.setItem(4, new ItemStack(Items.DIRT));
                helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "Conversion requires the native materials");
            }
        }
        helper.succeed();
    }

    private static void verifyRecipe(GameTestHelper helper, net.minecraft.world.item.crafting.Recipe<?> recipe, String prefix) {
        var menu = new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        };
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 3, 3);
        int width = recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped ? shaped.getRecipeWidth() : 3;
        for (int index = 0; index < recipe.getIngredients().size(); index++) {
            var ingredient = recipe.getIngredients().get(index);
            if (ingredient.isEmpty()) continue;
            var stacks = ingredient.getItems();
            helper.assertTrue(stacks.length > 0, "All crafting ingredients exist: " + recipe.getId());
            grid.setItem(index % width + index / width * 3, stacks[0].copy());
        }
        @SuppressWarnings("unchecked")
        var crafting = (net.minecraft.world.item.crafting.Recipe<net.minecraft.world.inventory.CraftingContainer>)recipe;
        helper.assertTrue(crafting.matches(grid, helper.getLevel()), "Crafting inputs match: " + recipe.getId());
        var result = crafting.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(!result.isEmpty() && ForgeRegistries.ITEMS.getKey(result.getItem()).getPath().startsWith("storage_") == !prefix.isEmpty(),
                "Crafting produces the correct upgrade family: " + recipe.getId());
        if (recipe instanceof org.brahypno.sophisticated_smeltery.recipe.VisibleControllerBindingRecipe visible) {
            var serializer = new org.brahypno.sophisticated_smeltery.recipe.VisibleControllerBindingRecipe.Serializer();
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                serializer.toNetwork(buffer, visible);
                var synced = serializer.fromNetwork(recipe.getId(), buffer);
                helper.assertTrue(ItemStack.isSameItemSameTags(synced.getResultItem(helper.getLevel().registryAccess()), visible.getResultItem(helper.getLevel().registryAccess())),
                        "Controller recipe synchronization retains host family");
            } finally { buffer.release(); }
        }
    }
}
