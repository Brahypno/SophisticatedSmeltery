package org.brahypno.sophisticated_smeltery.furnace;

import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModule;

/** A recipe view of a batch still owned by the backpack. Never an item inventory. */
public final class BackpackSlotInput {
    private final IItemHandler inventory;
    private final int slot;
    private final int batchLimit;
    private final MeltingModule module;
    private ItemStack view = ItemStack.EMPTY;
    private int lastCount;

    public BackpackSlotInput(IItemHandler inventory, int slot, int batchLimit, MeltingModule module) {
        this.inventory = inventory;
        this.slot = slot;
        this.batchLimit = batchLimit;
        this.module = module;
        ((BoundMeltingModule)module).sophisticatedsmeltery$bind(this);
    }

    public int slot() { return slot; }

    public void refresh() {
        ItemStack current = slot < inventory.getSlots() ? inventory.getStackInSlot(slot) : ItemStack.EMPTY;
        int count = Math.min(current.getCount(), batchLimit);
        if (count == 0) {
            if (!view.isEmpty()) module.setStack(ItemStack.EMPTY);
            view = ItemStack.EMPTY;
        } else if (!ItemStack.isSameItemSameTags(current, view) || view.getCount() != count
                || current.getCount() < lastCount || module.getStack().isEmpty()) {
            view = current.copyWithCount(count);
            module.set(0, 0);
            module.setStack(view);
        }
        lastCount = current.getCount();
    }

    public boolean finish(Predicate<IMeltingRecipe> nativeOutput, IMeltingRecipe recipe) {
        if (view.isEmpty() || !ItemStack.matches(inventory.extractItem(slot, view.getCount(), true), view)) return false;
        // These actions run together on the server thread. Retain the native
        // controller's output/byproduct implementation and capacity checks.
        if (!nativeOutput.test(recipe)) return false;
        ItemStack consumed = inventory.extractItem(slot, view.getCount(), false);
        if (!ItemStack.matches(consumed, view)) throw new IllegalStateException("Backpack input changed during melting completion");
        return true;
    }

    public void refreshRecipe() {
        int time = module.getCurrentTime() < 0 ? module.getRequiredTime() : Math.max(0, module.getCurrentTime());
        ((BoundMeltingModule)module).sophisticatedsmeltery$invalidateRecipe();
        module.setStack(view);
        module.set(0, Math.min(time, module.getRequiredTime()));
    }

    public boolean isEmpty() { return view.isEmpty(); }

    public CompoundTag saveProgress() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("slot", slot);
        // Matching fingerprint only: no inventory contents are restored from this tag.
        tag.put("inputKey", view.save(new CompoundTag()));
        tag.putInt("time", module.getCurrentTime());
        tag.putInt("required", module.getRequiredTime());
        tag.putInt("temperature", module.getRequiredTemp());
        return tag;
    }

    public void restoreProgress(CompoundTag tag) {
        refresh();
        if (ItemStack.matches(view, ItemStack.of(tag.getCompound("inputKey")))) module.set(0, tag.getInt("time"));
    }
}
