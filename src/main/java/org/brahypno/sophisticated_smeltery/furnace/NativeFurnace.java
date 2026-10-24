package org.brahypno.sophisticated_smeltery.furnace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.brahypno.sophisticated_smeltery.mixin.HeatingControllerAccess;
import org.brahypno.sophisticated_smeltery.mixin.HeatingMaterialsAccess;
import org.brahypno.sophisticated_smeltery.mixin.MultiblockShapeAccess;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock;

/** One detached native controller per installed upgrade, not per input or tick. */
public final class NativeFurnace {
    private static volatile int recipeEpoch;
    private int loadedRecipeEpoch = recipeEpoch;
    private final HeatingStructureBlockEntity controller;
    private final HeatingStructureMultiblock<?> nativeDimensions;
    private final MultiblockShapeAccess shape;
    private final HeatingMaterialsAccess materials;
    private final Runnable changed;
    private final Map<Integer, FurnaceDimensions> sizes = new HashMap<>();
    private final List<BackpackSlotInput> inputs = new ArrayList<>();
    private final Map<Integer, CompoundTag> savedProgress = new HashMap<>();
    private final PortableFuelModule fuel;
    @Nullable private final NativeAlloying alloying;
    private FurnaceDimensions dimensions = FurnaceDimensions.EMPTY;
    private int[] boundSlots = new int[0];
    private IItemHandler boundInventory;
    private int baseCapacity;

    public NativeFurnace(ItemStack controllerStack, CompoundTag state, Supplier<List<IFluidHandler>> fuelSources, Runnable changed) {
        this(ControllerBinding.create(controllerStack), state, fuelSources, changed);
    }

    public NativeFurnace(HeatingStructureBlockEntity controller, CompoundTag state,
                         Supplier<List<IFluidHandler>> fuelSources, Runnable changed) {
        if (controller == null) throw new IllegalArgumentException("Not a supported heating controller");
        this.controller = controller;
        this.changed = changed;
        ((PortableController)controller).sophisticatedsmeltery$attach(this::changed);
        nativeDimensions = ((HeatingControllerAccess)controller).sophisticatedsmeltery$getMultiblock();
        shape = (MultiblockShapeAccess)nativeDimensions;
        materials = (HeatingMaterialsAccess)nativeDimensions;
        alloying = NativeAlloying.find(controller);
        fuel = new PortableFuelModule(controller, fuelSources);
        fuel.readFromTag(state.getCompound("fuel"));
        controller.getTank().read(state.getCompound("tank"));
        baseCapacity = state.getInt("baseCapacity");
        dimensions = new FurnaceDimensions(state.getInt("x"), state.getInt("y"), state.getInt("z"),
                state.getInt("materials"), baseCapacity);
        for (Tag tag : state.getList("progress", Tag.TAG_COMPOUND)) {
            CompoundTag progress = (CompoundTag)tag;
            savedProgress.put(progress.getInt("slot"), progress.copy());
        }
    }

    public HeatingStructureBlockEntity controller() { return controller; }
    public MeltingModuleInventory melting() { return controller.getMeltingInventory(); }
    public PortableFuelModule fuel() { return fuel; }
    public int baseCapacity() { return baseCapacity; }
    public FurnaceDimensions dimensions() { return dimensions; }
    public boolean canAlloy() { return alloying != null; }
    public static void invalidateRecipes() { recipeEpoch++; }

    public boolean refreshRecipes() {
        if (loadedRecipeEpoch == recipeEpoch) return false;
        loadedRecipeEpoch = recipeEpoch;
        sizes.clear();
        for (BackpackSlotInput input : inputs) {
            input.refresh();
            input.refreshRecipe();
        }
        if (alloying != null) alloying.module().clearCachedRecipes();
        return true;
    }

    public boolean acceptsMaterial(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return false;
        return materials.sophisticatedsmeltery$isMaterial(item.getBlock())
                || materials.sophisticatedsmeltery$isFloor(item.getBlock())
                || materials.sophisticatedsmeltery$isWall(item.getBlock())
                || materials.sophisticatedsmeltery$isTank(item.getBlock());
    }

    public boolean isFuelTank(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && materials.sophisticatedsmeltery$isTank(item.getBlock());
    }

    public FurnaceDimensions calculate(int available) {
        return sizes.computeIfAbsent(available, count -> {
            // Capacity is obtained through the actual controller override, including
            // third-party constants. Only lightweight dimension data is supplied.
            Level level = controller.getLevel();
            int oldCapacity = controller.getTank().getCapacity();
            PortableController portable = (PortableController)controller;
            portable.sophisticatedsmeltery$setSizing(true);
            controller.setLevel(null);
            try {
                FurnaceDimensions result = FurnaceDimensions.largest(count, shape.sophisticatedsmeltery$innerLimit(),
                        nativeDimensions.getMaxHeight(), shape.sophisticatedsmeltery$hasFrame(),
                        shape.sophisticatedsmeltery$hasFloor(), shape.sophisticatedsmeltery$hasCeiling(),
                        (x, y, z) -> {
                            applyDimensions(x, y, z);
                            return controller.getTank().getCapacity();
                        });
                applyDimensions(dimensions.x(), dimensions.y(), dimensions.z());
                return result;
            } finally {
                controller.getTank().setCapacity(oldCapacity);
                controller.setLevel(level);
                portable.sophisticatedsmeltery$setSizing(false);
            }
        });
    }

    private void applyDimensions(int x, int y, int z) {
        if (x == 0 || y == 0 || z == 0) {
            ((HeatingControllerAccess)controller).sophisticatedsmeltery$applyDimensions(null);
            controller.getTank().setCapacity(0);
            return;
        }
        int caps = (shape.sophisticatedsmeltery$hasFloor() ? 1 : 0) + (shape.sophisticatedsmeltery$hasCeiling() ? 1 : 0);
        ((HeatingControllerAccess)controller).sophisticatedsmeltery$applyDimensions(nativeDimensions.createClient(
                BlockPos.ZERO, new BlockPos(x + 1, y + caps - 1, z + 1), List.of()));
    }

    public void resize(int materialCount, double capacityMultiplier) {
        FurnaceDimensions next = calculate(materialCount);
        PortableController portable = (PortableController)controller;
        portable.sophisticatedsmeltery$setSizing(true);
        Level level = controller.getLevel();
        controller.setLevel(null);
        try {
            dimensions = next;
            applyDimensions(next.x(), next.y(), next.z());
            baseCapacity = controller.getTank().getCapacity();
            controller.getTank().setCapacity((int)Math.min(Integer.MAX_VALUE, baseCapacity * capacityMultiplier));
        } finally {
            controller.setLevel(level);
            portable.sophisticatedsmeltery$setSizing(false);
        }
        changed();
    }

    public void bindInputs(Level level, IItemHandler inventory, int[] slots) {
        controller.setLevel(level);
        if (inventory == boundInventory && java.util.Arrays.equals(slots, boundSlots)) return;
        for (BackpackSlotInput input : inputs) savedProgress.put(input.slot(), input.saveProgress());
        inputs.clear();
        // Overflow stacks here are non-owning recipe views, not actual items.
        melting().resize(slots.length, ignored -> {});
        boundSlots = slots.clone();
        boundInventory = inventory;
        for (int index = 0; index < slots.length; index++) {
            BackpackSlotInput input = new BackpackSlotInput(inventory, slots[index], melting().getSlotLimit(index), melting().getModule(index));
            inputs.add(input);
            CompoundTag progress = savedProgress.remove(slots[index]);
            if (progress != null) input.restoreProgress(progress);
            else input.refresh();
        }
        savedProgress.keySet().retainAll(java.util.Arrays.stream(slots).boxed().toList());
    }

    public void refreshInputs() { inputs.forEach(BackpackSlotInput::refresh); }

    public CompoundTag progressAt(int slot) {
        for (BackpackSlotInput input : inputs) if (input.slot() == slot) return input.saveProgress();
        return savedProgress.getOrDefault(slot, new CompoundTag());
    }

    public void tick(boolean heating, boolean alloyEnabled, long gameTime) {
        switch ((int)(gameTime & 3)) {
            case 0 -> {
                if (heating && !fuel.hasFuel()) {
                    int temperature = fuel.findFuel(false);
                    boolean alloyNeedsFuel = false;
                    if (alloyEnabled && alloying != null) {
                        alloying.tank().setTemperature(temperature);
                        alloyNeedsFuel = alloying.module().canAlloy();
                    }
                    if (melting().canHeat(temperature) || alloyNeedsFuel) fuel.findFuel(true);
                }
            }
            case 1 -> {
                boolean hadProgress = false;
                for (int index = 0; index < melting().getSlots(); index++) {
                    if (melting().getCurrentTime(index) > 0) { hadProgress = true; break; }
                }
                if (heating && fuel.hasFuel()) melting().heatItems(fuel.getTemperature(), fuel.getRate());
                else if (heating) melting().coolItems();
                else {
                    // A disabled controller must not turn completed NO_SPACE batches
                    // into fluid. Native coolItem() retries those, so only cool work
                    // that has not finished heating.
                    for (int index = 0; index < melting().getSlots(); index++) {
                        if (melting().getCurrentTime(index) >= 0) melting().getModule(index).coolItem();
                    }
                }
                if (hadProgress || (heating && fuel.hasFuel() && melting().canHeat(fuel.getTemperature()))) changed();
            }
            case 2 -> {
                if (heating && alloyEnabled && alloying != null && fuel.hasFuel()) {
                    alloying.tank().setTemperature(fuel.getTemperature());
                    alloying.module().doAlloy();
                }
            }
            case 3 -> {
                // The user's heating switch pauses already-burning fuel as well.
                if (heating && fuel.hasFuel()) fuel.decreaseFuel(((HeatingControllerAccess)controller).sophisticatedsmeltery$getFuelRate());
            }
        }
    }

    private void changed() {
        changed.run();
    }

    public CompoundTag save() {
        CompoundTag state = new CompoundTag();
        state.put("tank", controller.getTank().write(new CompoundTag()));
        state.put("fuel", fuel.writeToTag(new CompoundTag()));
        ListTag progress = new ListTag();
        inputs.forEach(input -> { if (!input.isEmpty()) progress.add(input.saveProgress()); });
        savedProgress.values().forEach(tag -> progress.add(tag.copy()));
        state.put("progress", progress);
        state.putInt("baseCapacity", baseCapacity);
        state.putInt("x", dimensions.x());
        state.putInt("y", dimensions.y());
        state.putInt("z", dimensions.z());
        state.putInt("materials", dimensions.materials());
        return state;
    }
}
