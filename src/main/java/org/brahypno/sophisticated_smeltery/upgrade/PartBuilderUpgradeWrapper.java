package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import slimeknights.tconstruct.common.SoundUtils;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.material.IMaterialValue;
import slimeknights.tconstruct.library.recipe.material.MaterialValue;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderContainer;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

/** Native part-builder recipes, including custom patterns, recycling, and material leftovers. */
public final class PartBuilderUpgradeWrapper extends UpgradeWrapperBase<PartBuilderUpgradeWrapper, PartBuilderUpgradeItem>
        implements IPartBuilderContainer {
    public static final int MATERIAL = 0, PATTERN = 1;
    private final ItemStackHandler inventory;
    private Level level;
    private Pattern selected;
    private Map<Pattern, IPartBuilderRecipe> choices = Map.of();
    private List<Pattern> patterns = List.of();
    private static volatile int recipeEpoch;
    private int loadedRecipeEpoch = -1;
    private boolean dirty = true, loading = true;
    private int revision;
    private long lastSoundTick = Long.MIN_VALUE;

    public PartBuilderUpgradeWrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) {
        super(storage, upgrade, save);
        inventory = new ItemStackHandler(2) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return slot == MATERIAL || stack.is(TinkerTags.Items.PATTERNS);
            }
            @Override protected void onContentsChanged(int slot) {
                dirty = true;
                revision++;
                if (!loading) persist();
            }
        };
        if (upgrade.hasTag()) {
            if (upgrade.getTag().contains("partBuilderInventory", Tag.TAG_COMPOUND))
                inventory.deserializeNBT(upgrade.getTag().getCompound("partBuilderInventory"));
            selected = Pattern.tryParse(upgrade.getTag().getString("partBuilderPattern"));
        }
        loading = false;
    }
    @Override public boolean canBeDisabled() { return false; }
    public ItemStackHandler inventory() { return inventory; }
    public int revision() { refresh(); return revision; }
    public void bind(Level level) {
        if (this.level != level) { this.level = level; dirty = true; revision++; }
    }
    @Override public ItemStack getStack() { return inventory.getStackInSlot(MATERIAL); }
    @Override public ItemStack getPatternStack() { return inventory.getStackInSlot(PATTERN); }

    @Override @Nullable public IMaterialValue getMaterial() {
        ItemStack stack = getStack();
        if (level == null || stack.isEmpty()) return null;
        if (stack.is(TinkerTags.Items.TOOL_PARTS)) {
            var material = IMaterialItem.getMaterialFromStack(stack);
            int cost = MaterialCastingLookup.getItemCost(stack.getItem());
            return cost == 0 || MaterialId.UNKNOWN.matchesVariant(material) ? null : new MaterialValue(material, cost);
        }
        return level.getRecipeManager().getRecipeFor(TinkerRecipeTypes.MATERIAL.get(), this, level).orElse(null);
    }

    public static void invalidateRecipes() { recipeEpoch++; }
    private void refresh() {
        if (level == null) return;
        if (loadedRecipeEpoch != recipeEpoch) { loadedRecipeEpoch = recipeEpoch; dirty = true; revision++; }
        if (!dirty) return;
        dirty = false;
        Map<Pattern, IPartBuilderRecipe> available = new LinkedHashMap<>();
        if (!getPatternStack().isEmpty()) level.getRecipeManager().getAllRecipesFor(TinkerRecipeTypes.PART_BUILDER.get()).stream()
                .filter(recipe -> recipe.partialMatch(this)).sorted(Comparator.comparing(IPartBuilderRecipe::getId))
                .forEach(recipe -> recipe.getPatterns(this).forEach(pattern -> available.putIfAbsent(pattern, recipe)));
        choices = available;
        patterns = available.keySet().stream().sorted(Comparator.<Pattern>comparingInt(pattern -> available.get(pattern).getCost())
                .thenComparing(Comparator.naturalOrder())).toList();
    }
    public List<Pattern> patterns() { refresh(); return patterns; }
    @Nullable public Pattern selected() { return selected; }
    @Nullable public IPartBuilderRecipe recipe(Pattern pattern) { refresh(); return choices.get(pattern); }
    @Nullable public IPartBuilderRecipe selectedRecipe() { return selected == null ? null : recipe(selected); }
    public void select(@Nullable Pattern pattern) {
        refresh();
        if (pattern != null && !choices.containsKey(pattern)) return;
        selected = pattern;
        revision++;
        persist();
    }
    public ItemStack result() {
        var recipe = selectedRecipe();
        return level != null && recipe != null && recipe.matches(this, level)
                ? recipe.assemble(this, level.registryAccess(), selected) : ItemStack.EMPTY;
    }

    /** Consume once per taken result, following PartBuilderBlockEntity.onCraft. */
    public void craft(Player player, ItemStack result) {
        var recipe = selectedRecipe();
        if (level == null || recipe == null || !recipe.matches(this, level)) return;
        // Read both before changing either input: recipes may depend on the original stack.
        int used = recipe.getItemsUsed(this);
        ItemStack leftover = recipe.getLeftover(this, selected);
        result.onCraftedBy(level, player, result.getCount());
        ForgeEventFactory.firePlayerCraftingEvent(player, result, this);
        if (!level.isClientSide) {
            ItemHandlerHelper.giveItemToPlayer(player, leftover);
            if (lastSoundTick != level.getGameTime()) {
                lastSoundTick = level.getGameTime();
                SoundUtils.playSoundForAll(player, Sounds.SAW.getSound(), 0.8f, 0.8f + 0.4f * player.getRandom().nextFloat());
            }
        }
        shrink(MATERIAL, used, player);
        if (!getPatternStack().is(TinkerTags.Items.REUSABLE_PATTERNS)) shrink(PATTERN, 1, player);
    }
    private void shrink(int slot, int amount, Player player) {
        if (amount <= 0) return;
        ItemStack stack = inventory.getStackInSlot(slot);
        ItemStack remainder = stack.getCraftingRemainingItem().copy();
        remainder.setCount(remainder.getCount() * amount);
        if (stack.getCount() <= amount) inventory.setStackInSlot(slot, remainder);
        else {
            inventory.extractItem(slot, amount, false);
            if (!player.level().isClientSide) ItemHandlerHelper.giveItemToPlayer(player, remainder);
        }
    }
    public boolean shiftIntoStorage() { return upgrade.getOrCreateTag().getBoolean("partBuilderShiftIntoStorage"); }
    public void setShiftIntoStorage(boolean value) { upgrade.getOrCreateTag().putBoolean("partBuilderShiftIntoStorage", value); save(); }
    public void persist() {
        var tag = upgrade.getOrCreateTag();
        tag.put("partBuilderInventory", inventory.serializeNBT());
        if (selected == null) tag.remove("partBuilderPattern");
        else tag.putString("partBuilderPattern", selected.toString());
        save();
    }
    public static boolean cleanForClearing(ItemStack stack) {
        return !stack.hasTag() || stack.getTag().getCompound("partBuilderInventory").getList("Items", Tag.TAG_COMPOUND).isEmpty();
    }
}

