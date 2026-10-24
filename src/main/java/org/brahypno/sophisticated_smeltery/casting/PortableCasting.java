package org.brahypno.sophisticated_smeltery.casting;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;

/** Native casting recipes without a world block, world ticker or a second copy of the furnace tank. */
public final class PortableCasting {
    public enum Mode {
        TABLE, BASIN;
        public RecipeType<ICastingRecipe> recipes() {
            return this == TABLE ? TinkerRecipeTypes.CASTING_TABLE.get() : TinkerRecipeTypes.CASTING_BASIN.get();
        }
        private TagKey<Item> emptyCasts() {
            return this == TABLE ? TinkerTags.Items.TABLE_EMPTY_CASTS : TinkerTags.Items.BASIN_EMPTY_CASTS;
        }
    }

    private static int recipeEpoch;
    private final Runnable changed;
    private final ItemStackHandler inventory;
    private Mode mode = Mode.TABLE;
    private FluidStack fluid = FluidStack.EMPTY;
    private FluidStack recipeFluid = FluidStack.EMPTY;
    @Nullable private Level level;
    @Nullable private ICastingRecipe recipe;
    @Nullable private ResourceLocation recipeId;
    private int loadedEpoch = -1;
    private int required;
    private int coolingTime;
    private int timer;
    private boolean loading = true;
    private ItemStack previewInput = ItemStack.EMPTY;
    private FluidStack previewFluid = FluidStack.EMPTY;
    @Nullable private ICastingRecipe previewRecipe;
    @Nullable private Level previewLevel;
    private Mode previewMode;
    private int previewEpoch = -1;
    private final ICastingContainer context = new ICastingContainer() {
        @Override public ItemStack getStack() {
            ItemStack stack = inventory.getStackInSlot(0);
            return stack.is(mode.emptyCasts()) ? ItemStack.EMPTY : stack;
        }
        @Override public Fluid getFluid() { return recipeFluid.getFluid(); }
        @Override public CompoundTag getFluidTag() { return recipeFluid.getTag(); }
    };

    public PortableCasting(CompoundTag state, Runnable changed) {
        this.changed = changed;
        inventory = new ItemStackHandler(2) {
            @Override public int getSlotLimit(int slot) { return slot == 0 ? 1 : 64; }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && fluid.isEmpty(); }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return fluid.isEmpty() ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }
            @Override protected void onContentsChanged(int slot) {
                if (!loading) {
                    if (fluid.isEmpty()) clearRecipe();
                    changed.run();
                }
            }
        };
        mode = state.getBoolean("basin") ? Mode.BASIN : Mode.TABLE;
        fluid = FluidStack.loadFluidStackFromNBT(state.getCompound("fluid"));
        recipeFluid = fluid;
        if (state.contains("inventory")) inventory.deserializeNBT(state.getCompound("inventory"));
        timer = state.getInt("timer");
        recipeId = ResourceLocation.tryParse(state.getString("recipe"));
        loading = false;
    }

    public static void invalidateRecipes() { recipeEpoch++; }
    public ItemStackHandler inventory() { return inventory; }
    public Mode mode() { return mode; }
    public FluidStack fluid() { return fluid.copy(); }
    public boolean hasFluid() { return !fluid.isEmpty(); }
    public int required() { return required; }
    public int timer() { return timer; }
    public int coolingTime() { return coolingTime; }
    public boolean isCooling() { return recipe != null && required > 0 && fluid.getAmount() >= required; }

    public boolean setMode(Mode next) {
        if (!fluid.isEmpty()) return false;
        if (next != mode) { mode = next; clearRecipe(); changed.run(); }
        return true;
    }

    private void clearRecipe() {
        recipe = null;
        recipeId = null;
        required = 0;
        coolingTime = 0;
        timer = 0;
    }

    public void bind(Level next) {
        if (level == next && loadedEpoch == recipeEpoch) return;
        level = next;
        loadedEpoch = recipeEpoch;
        recipe = null;
        required = 0;
        coolingTime = 0;
        if (recipeId != null && !fluid.isEmpty()) {
            recipeFluid = fluid;
            recipe = next.getRecipeManager().byKey(recipeId).filter(ICastingRecipe.class::isInstance)
                    .map(ICastingRecipe.class::cast).filter(found -> found.getType() == mode.recipes()
                            && found.matches(context, next)).orElse(null);
            if (recipe != null) updateRecipeAmounts();
            else { timer = 0; changed.run(); }
        }
    }

    private void updateRecipeAmounts() {
        required = recipe.getFluidAmount(context);
        coolingTime = Math.max(0, recipe.getCoolingTime(context));
    }

    /** Pour only from the source's currently selected/bottom fluid, up to one native recipe. */
    public int pour(Level level, IFluidHandler source) {
        bind(level);
        if (isCooling() || !inventory.getStackInSlot(1).isEmpty()) return 0;
        FluidStack available = source.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty() || (!fluid.isEmpty() && !available.isFluidEqual(fluid))) return 0;
        if (fluid.isEmpty()) {
            recipeFluid = available;
            recipe = level.getRecipeManager().getRecipeFor(mode.recipes(), context, level).orElse(null);
            if (recipe == null) return 0;
            updateRecipeAmounts();
        }
        if (recipe == null || required <= 0) return 0;
        int amount = Math.min(available.getAmount(), required - fluid.getAmount());
        if (amount <= 0) return 0;
        FluidStack drained = source.drain(new FluidStack(available, amount), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return 0;
        if (fluid.isEmpty()) fluid = drained.copy();
        else fluid.grow(drained.getAmount());
        recipeFluid = fluid;
        recipeId = recipe.getId();
        changed.run();
        return drained.getAmount();
    }

    /** Return partial or invalidated casts; a running valid recipe cannot be cancelled here. */
    public int returnFluid(Level level, IFluidHandler target) {
        bind(level);
        if (fluid.isEmpty() || isCooling()) return 0;
        int accepted = target.fill(fluid, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return 0;
        int filled = target.fill(new FluidStack(fluid, accepted), IFluidHandler.FluidAction.EXECUTE);
        fluid.shrink(filled);
        if (fluid.isEmpty()) { fluid = FluidStack.EMPTY; clearRecipe(); }
        recipeFluid = fluid;
        if (filled > 0) changed.run();
        return filled;
    }

    public void tick(Level level) {
        bind(level);
        if (!isCooling() || !inventory.getStackInSlot(1).isEmpty()) return;
        timer++;
        if (timer >= coolingTime) {
            recipeFluid = fluid;
            if (!recipe.matches(context, level)) { clearRecipe(); changed.run(); return; }
            ItemStack output = recipe.assemble(context, level.registryAccess());
            boolean consumed = recipe.isConsumed(context);
            boolean switchSlots = recipe.switchSlots();
            int used = required;
            // Exactly the native no-redstone slot policy, including dynamic cast consumption.
            loading = true;
            try {
                if (switchSlots) {
                    if (!consumed) inventory.setStackInSlot(1, inventory.getStackInSlot(0));
                    inventory.setStackInSlot(0, output);
                } else {
                    if (consumed) inventory.setStackInSlot(0, ItemStack.EMPTY);
                    inventory.setStackInSlot(1, output);
                }
            } finally { loading = false; }
            fluid.shrink(used);
            if (fluid.isEmpty()) fluid = FluidStack.EMPTY;
            recipeFluid = fluid;
            clearRecipe();
        }
        changed.run();
    }

    public ItemStack preview(Level level, FluidStack selected) {
        bind(level);
        if (recipe != null) return recipe.assemble(context, level.registryAccess());
        if (selected.isEmpty()) return ItemStack.EMPTY;
        FluidStack previous = recipeFluid;
        recipeFluid = selected;
        try {
            ItemStack input = inventory.getStackInSlot(0);
            if (previewLevel != level || previewEpoch != recipeEpoch || previewMode != mode
                    || !ItemStack.matches(previewInput, input) || !previewFluid.isFluidEqual(selected)) {
                previewLevel = level;
                previewEpoch = recipeEpoch;
                previewMode = mode;
                previewInput = input.copy();
                previewFluid = selected.copy();
                previewRecipe = level.getRecipeManager().getRecipeFor(mode.recipes(), context, level).orElse(null);
            }
            return previewRecipe == null ? ItemStack.EMPTY : previewRecipe.assemble(context, level.registryAccess());
        } finally { recipeFluid = previous; }
    }

    public CompoundTag save() {
        CompoundTag state = new CompoundTag();
        state.putBoolean("basin", mode == Mode.BASIN);
        state.put("inventory", inventory.serializeNBT());
        state.put("fluid", fluid.writeToNBT(new CompoundTag()));
        state.putInt("timer", timer);
        if (recipeId != null) state.putString("recipe", recipeId.toString());
        return state;
    }
}
