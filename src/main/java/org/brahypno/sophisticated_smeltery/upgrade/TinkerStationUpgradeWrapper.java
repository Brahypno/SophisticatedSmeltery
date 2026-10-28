package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import slimeknights.mantle.recipe.container.ISingleStackContainer;
import slimeknights.tconstruct.common.SoundUtils;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.IMutableTinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;

/** Portable inventory using Tinkers' workstation recipe contract. */
public final class TinkerStationUpgradeWrapper extends UpgradeWrapperBase<TinkerStationUpgradeWrapper, TinkerStationUpgradeItem>
        implements IMutableTinkerStationContainer {
    public boolean isHostStack(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.isSameItemSameTags(stack, storageWrapper.getWrappedStorageStack());
    }
    private final ItemStackHandler inventory;
    private Level level;
    private Player craftingPlayer;
    private ResourceLocation selection;
    private String itemName = "";
    private boolean loading = true, dirty = true;
    private int revision, loadedRecipeEpoch = -1;
    private static volatile int recipeEpoch;
    private ITinkerStationRecipe recipe;
    private LazyToolStack result;
    private Component error;
    private long lastSoundTick = Long.MIN_VALUE;

    public TinkerStationUpgradeWrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) {
        super(storage, upgrade, save);
        inventory = new ItemStackHandler(getInputCount() + 1) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                var layout = layout().getSlot(slot);
                return !layout.isHidden() && layout.isValid(stack);
            }
            @Override protected void onContentsChanged(int slot) { dirty = true; revision++; if (!loading) persist(); }
        };
        if (upgrade.hasTag()) {
            var tag = upgrade.getTag();
            // Keep this tier's slot count when restoring a basic inventory after an upgrade.
            for (var entry : tag.getCompound("tinkerStationInventory").getList("Items", Tag.TAG_COMPOUND)) {
                var item = (net.minecraft.nbt.CompoundTag)entry;
                int slot = item.getInt("Slot");
                if (slot >= 0 && slot < inventory.getSlots()) inventory.setStackInSlot(slot, ItemStack.of(item));
            }
            selection = ResourceLocation.tryParse(tag.getString("tinkerStationLayout"));
            itemName = tag.getString("tinkerStationName");
        }
        loading = false;
    }
    public boolean isAdvanced() { return upgradeItem.isAdvanced(); }
    @Override public boolean canBeDisabled() { return false; }
    public ItemStackHandler inventory() { return inventory; }
    @Override public int getInputCount() { return isAdvanced() ? 5 : 3; }
    @Override public ItemStack getTinkerableStack() { return inventory.getStackInSlot(0); }
    @Override public ItemStack getInput(int index) {
        return index < 0 || index >= getInputCount() ? ItemStack.EMPTY : inventory.getStackInSlot(index + 1);
    }
    @Override public void setInput(int index, ItemStack stack) {
        if (index >= 0 && index < getInputCount()) inventory.setStackInSlot(index + 1, stack);
    }
    @Override @Nullable public MaterialRecipe getInputMaterial(int index) {
        if (level == null || getInput(index).isEmpty()) return null;
        ISingleStackContainer input = () -> getInput(index);
        return level.getRecipeManager().getRecipeFor(TinkerRecipeTypes.MATERIAL.get(), input, level).orElse(null);
    }
    @Override public void giveItem(ItemStack stack) {
        if (craftingPlayer != null && !craftingPlayer.level().isClientSide) craftingPlayer.getInventory().placeItemBackInInventory(stack);
    }
    public void bind(Level level) {
        if (this.level != level) { this.level = level; dirty = true; revision++; }
    }
    private ResourceLocation defaultLayout() { return new ResourceLocation("tconstruct", isAdvanced() ? "tinkers_anvil" : "tinker_station"); }
    public StationSlotLayout layout() {
        var loader = StationSlotLayoutLoader.getInstance();
        var selected = selection == null ? StationSlotLayout.EMPTY : loader.get(selection);
        return selected == StationSlotLayout.EMPTY || selected.getInputCount() > getInputCount()
                || selected.isMain() && !selected.getName().equals(defaultLayout()) ? loader.get(defaultLayout()) : selected;
    }
    public List<StationSlotLayout> layouts() {
        var loader = StationSlotLayoutLoader.getInstance();
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(loader.get(defaultLayout())), loader.getSortedSlots().stream()
                .filter(layout -> layout.getInputCount() <= getInputCount())).toList();
    }
    public void select(ResourceLocation name) {
        if (layouts().stream().noneMatch(layout -> layout.getName().equals(name))) return;
        selection = name; revision++; persist();
    }
    public String itemName() { return itemName; }
    public void rename(String name) {
        itemName = net.minecraft.SharedConstants.filterText(name);
        if (itemName.length() > 50) itemName = itemName.substring(0, 50);
        dirty = true; revision++; persist();
    }
    public static void invalidateRecipes() { recipeEpoch++; }
    public int revision() { refresh(); return revision; }
    private void refresh() {
        if (level == null) return;
        if (loadedRecipeEpoch != recipeEpoch) { loadedRecipeEpoch = recipeEpoch; dirty = true; revision++; recipe = null; }
        if (!dirty) return;
        dirty = false; result = null; error = null;
        if (recipe == null || !recipe.matches(this, level))
            recipe = level.getRecipeManager().getRecipeFor(TinkerRecipeTypes.TINKER_STATION.get(), this, level).orElse(null);
        if (recipe == null) return;
        var validated = recipe.getValidatedResult(this, level.registryAccess());
        if (validated.isSuccess()) {
            result = validated.getResult();
            if (!itemName.isBlank()) TooltipUtil.setDisplayName(result.getStack(), itemName);
        } else if (validated.hasError()) error = validated.getMessage();
    }
    public ItemStack result() { refresh(); return result == null ? ItemStack.EMPTY : result.getStack().copy(); }
    @Nullable public Component error() { refresh(); return error; }
    public void craft(Player player, ItemStack taken) {
        refresh();
        if (level == null || recipe == null || result == null || taken.isEmpty()) return;
        var crafted = result;
        var craftingRecipe = recipe;
        int shrinkTool = getTinkerableStack().isEmpty() ? 0 : craftingRecipe.shrinkToolSlotBy(crafted, this);
        taken.onCraftedBy(level, player, taken.getCount());
        ForgeEventFactory.firePlayerCraftingEvent(player, taken, this);
        craftingPlayer = player;
        try { craftingRecipe.updateInputs(crafted, this, !level.isClientSide); }
        finally { craftingPlayer = null; }
        if (shrinkTool > 0) inventory.extractItem(0, shrinkTool, false);
        itemName = ""; dirty = true; revision++; persist();
        if (!level.isClientSide && lastSoundTick != level.getGameTime()) {
            lastSoundTick = level.getGameTime();
            SoundUtils.playSoundForAll(player, isAdvanced() ? SoundEvents.ANVIL_USE : Sounds.SAW.getSound(),
                    isAdvanced() ? 0.4f : 0.8f, 0.9f + 0.1f * player.getRandom().nextFloat());
        }
    }
    public boolean shiftIntoStorage() { return upgrade.getOrCreateTag().getBoolean("tinkerStationShiftIntoStorage"); }
    public void setShiftIntoStorage(boolean value) { upgrade.getOrCreateTag().putBoolean("tinkerStationShiftIntoStorage", value); save(); }
    public void persist() {
        var tag = upgrade.getOrCreateTag();
        tag.put("tinkerStationInventory", inventory.serializeNBT());
        if (selection != null) tag.putString("tinkerStationLayout", selection.toString());
        tag.putString("tinkerStationName", itemName);
        save();
    }
    public static boolean cleanForClearing(ItemStack stack) {
        return !stack.hasTag() || stack.getTag().getCompound("tinkerStationInventory").getList("Items", Tag.TAG_COMPOUND).isEmpty();
    }
}
