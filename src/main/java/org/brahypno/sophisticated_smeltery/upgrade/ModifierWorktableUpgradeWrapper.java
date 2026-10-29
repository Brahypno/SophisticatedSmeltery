package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import slimeknights.tconstruct.common.SoundUtils;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.ITinkerableContainer;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;

/** A portable worktable inventory; all modifier operations belong to native registered recipes. */
public final class ModifierWorktableUpgradeWrapper extends UpgradeWrapperBase<ModifierWorktableUpgradeWrapper, ModifierWorktableUpgradeItem>
        implements ITinkerableContainer.Mutable {
    private final ItemStackHandler inventory;
    private Level level;
    private Player craftingPlayer;
    private boolean loading = true, dirty = true;
    private int selected = -1, revision;
    private static volatile int recipeEpoch;
    private int loadedRecipeEpoch = recipeEpoch;
    private IModifierWorktableRecipe recipe;
    private List<ModifierEntry> options = List.of();
    private LazyToolStack result;
    private Component message = Component.empty();
    private long lastSoundTick = Long.MIN_VALUE;

    public ModifierWorktableUpgradeWrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) {
        super(storage, upgrade, save);
        inventory = new ItemStackHandler(3) {
            @Override protected void onContentsChanged(int slot) {
                selected = -1; dirty = true; revision++;
                if (!loading) persist();
            }
        };
        if (upgrade.hasTag()) {
            var tag = upgrade.getTag();
            for (var entry : tag.getCompound("modifierWorktableInventory").getList("Items", Tag.TAG_COMPOUND)) {
                var item = (net.minecraft.nbt.CompoundTag)entry;
                int slot = item.getInt("Slot");
                if (slot >= 0 && slot < inventory.getSlots()) inventory.setStackInSlot(slot, ItemStack.of(item));
            }
            if (tag.contains("modifierWorktableSelection", Tag.TAG_INT)) selected = tag.getInt("modifierWorktableSelection");
        }
        loading = false;
    }
    @Override public boolean canBeDisabled() { return false; }
    public ItemStackHandler inventory() { return inventory; }
    @Override public ItemStack getTinkerableStack() { return inventory.getStackInSlot(0); }
    @Override public int getInputCount() { return 2; }
    @Override public ItemStack getInput(int index) {
        return index >= 0 && index < getInputCount() ? inventory.getStackInSlot(index + 1) : ItemStack.EMPTY;
    }
    @Override public void setInput(int index, ItemStack stack) {
        if (index >= 0 && index < getInputCount()) inventory.setStackInSlot(index + 1, stack);
    }
    @Override public void giveItem(ItemStack stack) {
        if (craftingPlayer != null && !craftingPlayer.level().isClientSide) craftingPlayer.getInventory().placeItemBackInInventory(stack);
    }
    public boolean isHostStack(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.isSameItemSameTags(stack, storageWrapper.getWrappedStorageStack());
    }
    public void bind(Level level) {
        if (this.level != level) { this.level = level; dirty = true; revision++; }
    }
    public static void invalidateRecipes() { recipeEpoch++; }
    public int revision() { refresh(); return revision; }
    private void refresh() {
        if (level == null) return;
        if (loadedRecipeEpoch != recipeEpoch) {
            loadedRecipeEpoch = recipeEpoch; recipe = null; selected = -1; dirty = true; revision++;
            upgrade.getOrCreateTag().putInt("modifierWorktableSelection", -1); save();
        }
        if (!dirty) return;
        dirty = false; result = null;
        if (recipe == null || !recipe.matches(this, level))
            recipe = level.getRecipeManager().getRecipeFor(TinkerRecipeTypes.MODIFIER_WORKTABLE.get(), this, level).orElse(null);
        options = recipe == null ? List.of() : List.copyOf(recipe.getModifierOptions(this));
        message = recipe == null ? Component.empty() : recipe.getDescription(this);
        if (selected < 0 || selected >= options.size()) { selected = -1; return; }
        var validated = recipe.getResult(this, options.get(selected));
        if (validated.isSuccess()) { result = validated.getResult(); message = Component.empty(); }
        else if (validated.hasError()) message = validated.getMessage();
    }
    @Nullable public IModifierWorktableRecipe recipe() { refresh(); return recipe; }
    public List<ModifierEntry> options() { refresh(); return options; }
    public int selected() { refresh(); return selected; }
    public Component message() { refresh(); return message; }
    public ItemStack result() { refresh(); return result == null ? ItemStack.EMPTY : result.getStack().copy(); }
    public void select(int index) {
        refresh();
        if (index < -1 || index >= options.size()) return;
        selected = index; dirty = true; revision++; refresh(); persist();
    }
    public void craft(Player player, ItemStack taken) {
        refresh();
        if (level == null || recipe == null || result == null || selected < 0 || taken.isEmpty()) return;
        var crafted = result;
        var craftingRecipe = recipe;
        var modifier = options.get(selected);
        int shrinkTool = craftingRecipe.shrinkToolSlotBy(crafted);
        taken.onCraftedBy(level, player, taken.getCount());
        ForgeEventFactory.firePlayerCraftingEvent(player, taken, this);
        craftingPlayer = player;
        try { craftingRecipe.updateInputs(crafted, this, modifier, !level.isClientSide); }
        finally { craftingPlayer = null; }
        if (shrinkTool > 0) inventory.extractItem(0, shrinkTool, false);
        selected = -1; dirty = true; revision++; persist();
        if (!level.isClientSide && lastSoundTick != level.getGameTime()) {
            lastSoundTick = level.getGameTime();
            SoundUtils.playSoundForAll(player, Sounds.SAW.getSound(), 0.8f, 0.8f + 0.4f * player.getRandom().nextFloat());
        }
    }
    public boolean shiftIntoStorage() { return upgrade.getOrCreateTag().getBoolean("modifierWorktableShiftIntoStorage"); }
    public void setShiftIntoStorage(boolean value) { upgrade.getOrCreateTag().putBoolean("modifierWorktableShiftIntoStorage", value); save(); }
    private void persist() {
        var tag = upgrade.getOrCreateTag();
        tag.put("modifierWorktableInventory", inventory.serializeNBT());
        tag.putInt("modifierWorktableSelection", selected);
        save();
    }
    public static boolean cleanForClearing(ItemStack stack) {
        return !stack.hasTag() || stack.getTag().getCompound("modifierWorktableInventory").getList("Items", Tag.TAG_COMPOUND).isEmpty();
    }
}
