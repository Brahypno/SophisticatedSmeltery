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
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.item.TankItem;

/** Development-only fixtures, available with runClient -PmanualTest. */
@Mod.EventBusSubscriber(modid = "sophisticatedsmeltery")
public final class ManualTestCommand {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ssmeltery_test")
                .requires(source -> source.hasPermission(2))
                .executes(context -> give(context, false))
                .then(Commands.literal("smeltery").executes(context -> give(context, false)))
                .then(Commands.literal("foundry").executes(context -> give(context, true))));
    }

    private static int give(CommandContext<CommandSourceStack> context, boolean foundry) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack backpack = new ItemStack(ModItems.NETHERITE_BACKPACK.get());
        var storage = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(IllegalStateException::new);
        storage.getInventoryHandler();
        var controller = foundry ? TinkerSmeltery.foundryController : TinkerSmeltery.smelteryController;
        storage.getUpgradeHandler().setStackInSlot(0, ControllerBinding.bind(
                new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()), new ItemStack(controller)));
        var wrapper = (MeltingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
        wrapper.setHeating(false);
        wrapper.getComponents().setStackInSlot(0, new ItemStack(foundry ? TinkerSmeltery.scorchedBricks : TinkerSmeltery.searedBricks, 32));
        ItemStack tank = new ItemStack((foundry ? TinkerSmeltery.scorchedTank : TinkerSmeltery.searedTank).get(SearedTankBlock.TankType.FUEL_TANK));
        TankItem.setTank(tank, new FluidStack(Fluids.LAVA, 1000));
        wrapper.getComponents().setStackInSlot(1, tank);
        wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenIron.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        wrapper.getTank().fill(new FluidStack(TinkerFluids.moltenGold.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        wrapper.persist();
        storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 3));
        storage.getInventoryHandler().setStackInSlot(1, new ItemStack(Items.RAW_IRON, 3));
        storage.getInventoryHandler().setStackInSlot(2, new ItemStack(Items.GOLD_INGOT, 3));
        player.addItem(backpack);
        player.addItem(new ItemStack(SophisticatedSmeltery.MELTING_UPGRADE.get()));
        player.addItem(new ItemStack(TinkerSmeltery.smelteryController));
        player.addItem(new ItemStack(TinkerSmeltery.foundryController));
        player.addItem(new ItemStack(Items.BUCKET, 8));
        context.getSource().sendSuccess(() -> Component.literal("Test backpack supplied. Heating is OFF. /ssmeltery_test foundry supplies the other controller."), false);
        return Command.SINGLE_SUCCESS;
    }
}
