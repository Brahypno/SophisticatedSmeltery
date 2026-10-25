package org.brahypno.sophisticated_smeltery.furnace;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.IMutableAlloyTank;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.smeltery.block.entity.controller.AlloyerBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.SingleAlloyingModule;

/** Native single-output alloying with inventory tanks replacing world neighbors. */
public final class PortableAlloyer {
    public static final int INPUTS = 8;
    public static final int FUEL_SLOT = 8;
    private static int recipeEpoch;
    private int loadedEpoch = recipeEpoch;
    private final AlloyerBlockEntity controller;
    private final FluidTankAnimated tank;
    private final int baseCapacity;
    private final PortableFuelModule fuel;
    private SingleAlloyingModule module;
    private ItemStackHandler components;
    private int temperature;
    private final IMutableAlloyTank alloyTank = new IMutableAlloyTank() {
        @Override public int getTemperature() { return temperature; }
        @Override public int getTanks() { return INPUTS; }
        @Override public FluidStack getFluidInTank(int index) {
            IFluidHandler input = input(index);
            return input == null ? FluidStack.EMPTY : input.getFluidInTank(0);
        }
        @Override public boolean canFit(FluidStack fluid, int removed) {
            return tank.fill(fluid, IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount();
        }
        @Override public FluidStack drain(int index, FluidStack fluid) {
            IFluidHandler input = input(index);
            if (input == null) return FluidStack.EMPTY;
            FluidStack result = input.drain(fluid, IFluidHandler.FluidAction.EXECUTE);
            if (!result.isEmpty()) ((PortableController)controller).sophisticatedsmeltery$changed();
            return result;
        }
        @Override public int fill(FluidStack fluid) { return tank.fill(fluid, IFluidHandler.FluidAction.EXECUTE); }
    };

    public PortableAlloyer(AlloyerBlockEntity controller, CompoundTag state, Supplier<List<IFluidHandler>> fuelSources, Runnable changed) {
        this.controller = controller;
        ((PortableController)controller).sophisticatedsmeltery$attach(changed);
        baseCapacity = controller.getTank().getCapacity();
        tank = controller.getTank();
        tank.readFromNBT(state.getCompound("tank"));
        fuel = new PortableFuelModule(controller, fuelSources, this::requiredTemperature);
        fuel.readFromTag(state.getCompound("fuel"));
        module = new SingleAlloyingModule(controller, alloyTank);
    }

    public static void invalidateRecipes() { recipeEpoch++; }
    public void bind(Level level) {
        controller.setLevel(level);
        if (loadedEpoch != recipeEpoch) {
            loadedEpoch = recipeEpoch;
            module = new SingleAlloyingModule(controller, alloyTank);
        }
    }
    public void setComponents(ItemStackHandler components) {
        this.components = components;
        fuel.setSolidSource(() -> components.getStackInSlot(FUEL_SLOT), stack -> components.setStackInSlot(FUEL_SLOT, stack));
    }
    public static boolean acceptsInput(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().is(TinkerTags.Blocks.ALLOYER_TANKS)
                && stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
    }
    public static boolean acceptsFuel(ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, TinkerRecipeTypes.FUEL.get()) / 4 > 0 || isFuelTank(stack);
    }
    public static boolean isFuelTank(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().is(TinkerTags.Blocks.FUEL_TANKS)
                && stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
    }
    public IFluidHandler input(int slot) {
        if (components == null || slot < 0 || slot >= INPUTS) return null;
        ItemStack stack = components.getStackInSlot(slot);
        return acceptsInput(stack) ? stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null) : null;
    }
    private int requiredTemperature(int maximum) {
        if (controller.getLevel() == null) return 0;
        temperature = maximum;
        return controller.getLevel().getRecipeManager().getRecipesFor(TinkerRecipeTypes.ALLOYING.get(), alloyTank, controller.getLevel())
                .stream().filter(recipe -> recipe.canPerform(alloyTank)).mapToInt(recipe -> recipe.getTemperature()).min().orElse(0);
    }
    public void resize(double multiplier) {
        tank.setCapacity((int)Math.min(Integer.MAX_VALUE, baseCapacity * multiplier));
    }
    public AlloyerBlockEntity controller() { return controller; }
    public FluidTankAnimated tank() { return tank; }
    public PortableFuelModule fuel() { return fuel; }
    public int baseCapacity() { return baseCapacity; }
    public int fluidRevision() { return ((PortableController)controller).sophisticatedsmeltery$getFluidRevision(); }
    public void tick(boolean enabled, long gameTime) {
        if (!enabled) return;
        if ((gameTime & 3) == 0) {
            fuel.heatWithFuelSelection(() -> {
                temperature = fuel.hasFuel() ? fuel.getTemperature() : fuel.findFuel(false);
                if (!fuel.hasFuel() && module.canAlloy()) fuel.findFuel(true);
            });
        } else if ((gameTime & 3) == 2 && fuel.hasFuel()) {
            temperature = fuel.getTemperature();
            module.doAlloy();
            fuel.decreaseFuel(1);
        }
    }
    public CompoundTag save() {
        CompoundTag state = new CompoundTag();
        state.put("tank", tank.writeToNBT(new CompoundTag()));
        state.put("fuel", fuel.writeToTag(new CompoundTag()));
        return state;
    }
}
