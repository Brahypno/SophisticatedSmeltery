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
                .then(Commands.literal("foundry").executes(context -> give(context, true))));
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
