package org.brahypno.sophisticated_smeltery;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import org.brahypno.sophisticated_smeltery.furnace.ControllerBinding;
import org.brahypno.sophisticated_smeltery.upgrade.MeltingUpgradeWrapper;
import org.brahypno.sophisticated_smeltery.upgrade.CastingUpgradeWrapper;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.item.TankItem;

/** Development-only fixtures, available with runClient -PmanualTest. */
@Mod.EventBusSubscriber(modid = SophisticatedSmeltery.MODID)
public final class ManualTestCommand {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ssmeltery_test")
                .requires(source -> source.hasPermission(2))
                .executes(context -> give(context, false))
                .then(Commands.literal("smeltery").executes(context -> give(context, false)))
                .then(Commands.literal("casting").executes(context -> give(context, false, true)))
                .then(Commands.literal("casting_expansion").executes(ManualTestCommand::giveCastingExpansion))
                .then(Commands.literal("alloyer").executes(ManualTestCommand::giveAlloyer))
                .then(Commands.literal("part_builder").executes(ManualTestCommand::givePartBuilder))
                .then(Commands.literal("modifier_worktable").executes(context -> giveModifierWorktable(context, false)))
                .then(Commands.literal("storage_modifier_worktable").executes(context -> giveModifierWorktable(context, true)))
                .then(Commands.literal("tinker_station").executes(context -> giveTinkerStation(context, false)))
                .then(Commands.literal("tinkers_anvil").executes(context -> giveTinkerStation(context, true)))
                .then(Commands.literal("tinker_info").executes(context -> giveTinkerStation(context, true, true)))
                .then(Commands.literal("spawner").executes(ManualTestCommand::giveSpawner))
                .then(Commands.literal("foundry").executes(context -> give(context, true))));
    }

    private static int giveTinkerStation(CommandContext<CommandSourceStack> context, boolean advanced) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return giveTinkerStation(context, advanced, false);
    }
    private static int giveTinkerStation(CommandContext<CommandSourceStack> context, boolean advanced, boolean information) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack backpack = new ItemStack(ModItems.BACKPACK.get());
        var storage = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(advanced ? SophisticatedSmeltery.TINKERS_ANVIL_UPGRADE.get() : SophisticatedSmeltery.TINKER_STATION_UPGRADE.get()));
        var parts = java.util.List.of(slimeknights.tconstruct.tools.TinkerToolParts.pickHead,
                slimeknights.tconstruct.tools.TinkerToolParts.toolHandle, slimeknights.tconstruct.tools.TinkerToolParts.toolBinding);
        for (int i = 0; i < parts.size(); i++) player.getInventory().setItem(11 + i,
                slimeknights.tconstruct.library.tools.part.IMaterialItem.withMaterial(new ItemStack(parts.get(i), 4),
                        slimeknights.tconstruct.library.materials.definition.MaterialVariantId.parse("tconstruct:iron")));
        if (information) {
            try {
                var item = slimeknights.tconstruct.tools.TinkerTools.pickaxe.get();
                var iron = slimeknights.tconstruct.library.materials.definition.MaterialVariant.of(
                        slimeknights.tconstruct.library.materials.definition.MaterialVariantId.parse("tconstruct:iron"));
                var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.createTool(item, item.getToolDefinition(),
                        new slimeknights.tconstruct.library.tools.nbt.MaterialNBT(java.util.List.of(iron, iron, iron)));
                for (String modifier : java.util.List.of("haste", "sharpness", "reinforced", "diamond", "emerald", "netherite",
                        "experienced", "magnetic", "soulbound", "silky", "smite", "knockback", "expanded", "hydraulic",
                        "lightspeed", "severing", "necrotic", "piercing", "autosmelt")) {
                    var id = new slimeknights.tconstruct.library.modifiers.ModifierId("tconstruct", modifier);
                    if (!slimeknights.tconstruct.library.modifiers.ModifierManager.INSTANCE.contains(id)) throw new IllegalStateException("Missing test modifier: " + id);
                    tool.addModifier(id, 1);
                }
                tool.setDamage(50);
                var workstation = (org.brahypno.sophisticated_smeltery.upgrade.TinkerStationUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
                workstation.inventory().setStackInSlot(0, tool.createStack());
            } catch (RuntimeException failure) {
                SophisticatedSmeltery.LOGGER.error("Workstation information fixture failed", failure);
                throw failure;
            }
        }
        player.getInventory().setItem(14, ItemStack.EMPTY);
        var equipment = java.util.List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.SHIELD);
        for (int index = 0; index < equipment.size(); index++) {
            player.getInventory().setItem(index < 4 ? 39 - index : 40, ItemStack.EMPTY);
            player.getInventory().setItem(15 + index, new ItemStack(equipment.get(index)));
        }
        player.getInventory().selected = 0; player.getInventory().setItem(0, backpack);
        player.inventoryMenu.broadcastChanges();
        backpack.getItem().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        return Command.SINGLE_SUCCESS;
    }
    private static int giveModifierWorktable(CommandContext<CommandSourceStack> context, boolean useStorage) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper storage;
        ItemStack backpack = new ItemStack(ModItems.BACKPACK.get());
        net.minecraft.core.BlockPos pos = player.blockPosition().offset(2, 0, 0);
        if (useStorage) {
            player.serverLevel().setBlockAndUpdate(pos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.NETHERITE_CHEST.get().defaultBlockState());
            storage = ((net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity)player.serverLevel().getBlockEntity(pos)).getStorageWrapper();
        } else storage = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(useStorage ? SophisticatedSmeltery.STORAGE_MODIFIER_WORKTABLE_UPGRADE.get() : SophisticatedSmeltery.MODIFIER_WORKTABLE_UPGRADE.get()));
        var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(ModifierWorktableUpgradeGameTests.tool());
        for (String name : java.util.List.of("sharpness", "reinforced", "emerald", "netherite", "experienced", "magnetic", "soulbound", "silky", "smite", "knockback", "expanded", "hydraulic", "lightspeed", "severing", "necrotic", "piercing", "autosmelt"))
            tool.addModifier(new slimeknights.tconstruct.library.modifiers.ModifierId("tconstruct", name), 1);
        player.getInventory().setItem(11, tool.createStack()); player.getInventory().setItem(12, new ItemStack(Items.COMPASS));
        for (int slot = 36; slot <= 40; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
        player.getInventory().selected = 0; player.getInventory().setItem(0, backpack);
        player.inventoryMenu.broadcastChanges();
        if (useStorage) {
            // Opening follows placement in this fixture; deliver the block before the native menu packet.
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(player.serverLevel(), pos));
            var update = player.serverLevel().getBlockEntity(pos).getUpdatePacket();
            if (update != null) player.connection.send(update);
            player.serverLevel().getBlockState(pos).use(player.serverLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
            if (player.containerMenu == player.inventoryMenu) throw new IllegalStateException("Storage fixture did not open its native menu at " + pos);
        }
        else backpack.getItem().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        return Command.SINGLE_SUCCESS;
    }
    private static int givePartBuilder(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack backpack = new ItemStack(ModItems.NETHERITE_BACKPACK.get());
        var storage = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(SophisticatedSmeltery.PART_BUILDER_UPGRADE.get()));
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, backpack);
        player.getInventory().setItem(11, new ItemStack(slimeknights.tconstruct.tables.TinkerTables.pattern, 16));
        player.getInventory().setItem(12, new ItemStack(Items.OAK_LOG, 16));
        player.inventoryMenu.broadcastChanges();
        backpack.getItem().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        return Command.SINGLE_SUCCESS;
    }

    private static int giveSpawner(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack backpack = new ItemStack(ModItems.NETHERITE_BACKPACK.get());
        var storage = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(SophisticatedSmeltery.ADVANCED_SPAWNER_UPGRADE.get()));
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
        SpawnerUpgradeGameTests.upgrade(storage).setEnabled(false);
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, backpack);
        player.getInventory().setItem(11, SpawnerUpgradeGameTests.spawner(player.blockPosition().above(4), "minecraft:pig", 1));
        player.getInventory().setItem(12, new ItemStack(SophisticatedSmeltery.SPAWNER_UPGRADE.get()));
        player.inventoryMenu.broadcastChanges();
        backpack.getItem().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        return Command.SINGLE_SUCCESS;
    }

    private static int giveAlloyer(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack backpack = new ItemStack(ModItems.NETHERITE_BACKPACK.get());
        var storage = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), new ItemStack(TinkerSmeltery.foundryController)));
        var furnace = (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
        furnace.getComponents().setStackInSlot(0, new ItemStack(TinkerSmeltery.scorchedBricks, 32));
        furnace.setHeating(false);
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(SophisticatedSmeltery.ALLOYER_UPGRADE.get()));
        var alloyer = (org.brahypno.sophisticated_smeltery.upgrade.AlloyerUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(1);
        alloyer.setHeating(false);
        for (int slot = 0; slot < 8; slot++) {
            ItemStack tank = new ItemStack(TinkerSmeltery.scorchedTank.get(SearedTankBlock.TankType.INGOT_TANK));
            if (slot >= 6) TankItem.setTank(tank, new FluidStack(slot == 6 ? TinkerFluids.moltenCopper.get() : TinkerFluids.moltenGold.get(), 90));
            alloyer.getComponents().setStackInSlot(slot, tank);
        }
        if (Boolean.getBoolean("sophisticatedsmeltery.alloyerLiquidSmoke")) {
            ItemStack fuelTank = new ItemStack(TinkerSmeltery.scorchedTank.get(SearedTankBlock.TankType.FUEL_TANK));
            TankItem.setTank(fuelTank, new FluidStack(Fluids.LAVA, 1000));
            alloyer.getComponents().setStackInSlot(8, fuelTank);
        } else alloyer.getComponents().setStackInSlot(8, new ItemStack(Items.COAL, 16));
        alloyer.getTank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        alloyer.persist();
        storage.getUpgradeHandler().setStackInSlot(2, new ItemStack(SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get()));
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, backpack);
        player.getInventory().setItem(11, new ItemStack(Items.BUCKET));
        player.inventoryMenu.broadcastChanges();
        backpack.getItem().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        return Command.SINGLE_SUCCESS;
    }

    private static int giveCastingExpansion(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack backpack = new ItemStack(ModItems.NETHERITE_BACKPACK.get());
        var storage = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
        storage.getInventoryHandler();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.TANK_UPGRADE.get()));
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(SophisticatedSmeltery.ADVANCED_SCORCHED_CASTING_UPGRADE.get()));
        storage.getUpgradeHandler().setStackInSlot(2, new ItemStack(SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get()));
        storage.getUpgradeHandler().setStackInSlot(3, new ItemStack(SophisticatedSmeltery.SCORCHED_CASTING_UPGRADE.get()));
        var tank = storage.getUpgradeHandler().getTypeWrappers(net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeItem.TYPE).iterator().next();
        tank.fill(new FluidStack(TinkerFluids.moltenIron.get(), 270), IFluidHandler.FluidAction.EXECUTE, false);
        ItemStack cast = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM
                .get(new net.minecraft.resources.ResourceLocation("tconstruct", "ingot_sand_cast")));
        ((CastingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(1)).casting().inventory().setStackInSlot(0, cast.copy());
        storage.getInventoryHandler().setStackInSlot(0, cast.copyWithCount(2));
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, backpack);
        player.getInventory().setItem(11, new ItemStack(ModItems.UPGRADE_BASE.get()));
        // Unlock both tier recipes through the same inventory criterion used in play.
        player.getInventory().setItem(12, new ItemStack(SophisticatedSmeltery.SCORCHED_CASTING_UPGRADE.get()));
        player.inventoryMenu.broadcastChanges();
        backpack.getItem().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        return Command.SINGLE_SUCCESS;
    }

    private static int give(CommandContext<CommandSourceStack> context, boolean foundry) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return give(context, foundry, Boolean.getBoolean("sophisticatedsmeltery.castingUpgradeSmoke"));
    }
    private static int give(CommandContext<CommandSourceStack> context, boolean foundry, boolean casting) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack backpack = new ItemStack(ModItems.NETHERITE_BACKPACK.get());
        var storage = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
        storage.getInventoryHandler();
        var controller = foundry ? TinkerSmeltery.foundryController : TinkerSmeltery.smelteryController;
        boolean advancedTransmute = Boolean.getBoolean("sophisticatedsmeltery.transmuteControlsSmoke");
        boolean transmute = Boolean.getBoolean("sophisticatedsmeltery.transmuteMobSmoke") || advancedTransmute;
        boolean missing = Boolean.getBoolean("sophisticatedsmeltery.missingControllerSmoke");
        ItemStack controllerStack = transmute ? transmuteItem("transmute_controller", 1) : new ItemStack(controller);
        ItemStack upgradeStack = ControllerBinding.bind(new ItemStack(advancedTransmute
                ? SophisticatedSmeltery.ADVANCED_TRANSMUTE_UPGRADE.get() : SophisticatedSmeltery.MELTING_UPGRADE.get()), controllerStack);
        if (missing) upgradeStack.getTag().getCompound(ControllerBinding.CONTROLLER).putString("id", "esotericism_tinker:transmute_controller");
        storage.getUpgradeHandler().setStackInSlot(0, upgradeStack);
        var wrapper = (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
        wrapper.setHeating(false);
        if (!missing) {
            wrapper.getComponents().setStackInSlot(0, transmute ? transmuteItem("ashen_bricks", 32)
                    : new ItemStack(foundry ? TinkerSmeltery.scorchedBricks : TinkerSmeltery.searedBricks, 32));
            ItemStack tank = transmute ? transmuteItem("ashen_fuel_tank", 1)
                    : new ItemStack((foundry ? TinkerSmeltery.scorchedTank : TinkerSmeltery.searedTank).get(SearedTankBlock.TankType.FUEL_TANK));
            TankItem.setTank(tank, new FluidStack(Fluids.LAVA, 1000));
            wrapper.getComponents().setStackInSlot(1, tank);
            wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenGold.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            wrapper.persist();
        }
        if (Boolean.getBoolean("sophisticatedsmeltery.heatingSwitchSmoke")) {
            if (advancedTransmute) {
                player.getInventory().setItem(11, ControllerBinding.bind(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), controllerStack));
                player.getInventory().setItem(12, upgradeStack.copy());
            }
            storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .get(new net.minecraft.resources.ResourceLocation("sophisticatedbackpacks", "advanced_magnet_upgrade"))));
        }
        if (casting) {
            storage.getUpgradeHandler().setStackInSlot(2, new ItemStack(SophisticatedSmeltery.ADVANCED_SEARED_CASTING_UPGRADE.get()));
            storage.getUpgradeHandler().setStackInSlot(3, new ItemStack(SophisticatedSmeltery.SEARED_CASTING_UPGRADE.get()));
            var castingWrapper = (CastingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(2);
            castingWrapper.casting().inventory().setStackInSlot(0, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .get(new net.minecraft.resources.ResourceLocation("tconstruct", "ingot_cast"))));
        }
        storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 3));
        storage.getInventoryHandler().setStackInSlot(1, new ItemStack(Items.RAW_IRON, 3));
        storage.getInventoryHandler().setStackInSlot(2, new ItemStack(Items.GOLD_INGOT, 3));
        if (Boolean.getBoolean("sophisticatedsmeltery.mobMeltingSmoke")) {
            storage.getUpgradeHandler().setStackInSlot(2, new ItemStack(ModItems.ADVANCED_MOB_CATCHER_UPGRADE.get()));
            storage.getInventoryHandler().setStackInSlot(0, ItemStack.EMPTY);
            var cow = net.minecraft.world.entity.EntityType.COW.create(player.serverLevel());
            if (Boolean.getBoolean("sophisticatedsmeltery.mobMeltingReloadSmoke")) {
                cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
                cow.setHealth(100);
            }
            var nbt = new net.minecraft.nbt.CompoundTag();
            cow.saveWithoutId(nbt);
            net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage.addCapturedMob(storage,
                    new net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob(java.util.UUID.randomUUID(),
                            new net.minecraft.resources.ResourceLocation("minecraft", "cow"), nbt, 0, 1, 2, 2,
                            false, "Cow", (int) cow.getHealth(), (int) cow.getMaxHealth()));
        }
        if (Boolean.getBoolean("sophisticatedsmeltery.heatingSwitchSmoke")) {
            if (Boolean.getBoolean("sophisticatedsmeltery.castingUpgradeSmoke"))
                player.getInventory().setItem(11, new ItemStack(ModItems.UPGRADE_BASE.get()));
            player.getInventory().selected = 0;
            player.getInventory().setItem(0, backpack);
            player.inventoryMenu.broadcastChanges();
            backpack.getItem().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        } else player.addItem(backpack);
        player.addItem(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()));
        player.addItem(new ItemStack(TinkerSmeltery.smelteryController));
        player.addItem(new ItemStack(TinkerSmeltery.foundryController));
        player.addItem(new ItemStack(Items.BUCKET, 8));
        context.getSource().sendSuccess(() -> Component.literal("Test backpack supplied. Heating is OFF. /ssmeltery_test foundry supplies the other controller."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static ItemStack transmuteItem(String name, int count) {
        return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation("esotericism_tinker", name)), count);
    }
}
