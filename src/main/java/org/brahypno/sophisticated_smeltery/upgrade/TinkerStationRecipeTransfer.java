package org.brahypno.sophisticated_smeltery.upgrade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;

/** Plans on either side and applies the server's plan to live slots only after all items have a destination. */
public final class TinkerStationRecipeTransfer {
    static final String MESSAGE = "sophisticatedSmelteryRecipeTransfer";

    public enum Error {
        TOO_LARGE("jei.tconstruct.tinker_station.too_large"),
        LAYOUT("gui.sophisticated_smeltery.tinker_transfer_layout"),
        MISSING("jei.tooltip.error.recipe.transfer.missing"),
        NO_ROOM("gui.sophisticated_smeltery.tinker_transfer_no_room");

        private final String key;

        Error(String key) { this.key = key; }

        public Component message() { return Component.translatable(key); }
    }

    public record Plan(int sets, List<ItemStack> ingredients, Map<Slot, ItemStack> changes) {}
    public record Result(Error error, Plan plan) {}

    private TinkerStationRecipeTransfer() {}

    public static void request(StorageContainerMenuBase<?> menu, TinkerStationUpgradeContainer station,
            ResourceLocation recipe, boolean modifier, Plan plan) {
        station.sendDataToServer(() -> {
            var request = new CompoundTag();
            request.putInt("menu", menu.containerId);
            request.putIntArray("targets", materialIds(station));
            request.put("station", stationState(station));
            request.putString("recipe", recipe.toString());
            request.putBoolean("modifier", modifier);
            request.putInt("sets", plan.sets());
            var ingredients = new ListTag();
            for (ItemStack stack : plan.ingredients()) ingredients.add(stack.save(new CompoundTag()));
            request.put("ingredients", ingredients);
            var data = new CompoundTag();
            data.put(MESSAGE, request);
            return data;
        });
    }

    static void handleRequest(TinkerStationUpgradeContainer station, CompoundTag request) {
        var player = station.getPlayer();
        if (player.level().isClientSide) return;
        if (!(player.containerMenu instanceof StorageContainerMenuBase<?> menu) || !request.contains("menu", Tag.TAG_INT)
                || menu.containerId != request.getInt("menu") || !menu.stillValid(player) || menu.hasSomethingMessedWithStorage()
                || menu.getOpenContainer().orElse(null) != station
                || !Arrays.equals(materialIds(station), request.getIntArray("targets"))
                || !stationState(station).equals(request.getCompound("station"))) {
            reject(player);
            return;
        }

        int sets = request.getInt("sets");
        var ingredients = request.getList("ingredients", Tag.TAG_COMPOUND);
        var id = ResourceLocation.tryParse(request.getString("recipe"));
        if (sets < 1 || sets > 64 || ingredients.size() != station.materialSlots().size() || id == null) {
            reject(player);
            return;
        }
        List<ItemStack> selected = new ArrayList<>(ingredients.size());
        for (int i = 0; i < ingredients.size(); i++) selected.add(ItemStack.of(ingredients.getCompound(i)));
        if (!matchesRecipe(player, id, request.getBoolean("modifier"), selected)) {
            reject(player);
            return;
        }

        List<List<ItemStack>> inputs = selected.stream().map(stack -> stack.isEmpty() ? List.<ItemStack>of() : List.of(stack)).toList();
        var result = plan(menu, station, inputs, sets, sets, true);
        if (result.plan() == null) {
            player.displayClientMessage(result.error().message(), true);
            return;
        }
        result.plan().changes().forEach(Slot::set);
        menu.broadcastChanges();
    }

    private static void reject(Player player) {
        player.displayClientMessage(Component.translatable("gui.sophisticated_smeltery.tinker_transfer_changed"), true);
    }

    private static int[] materialIds(TinkerStationUpgradeContainer station) {
        return station.materialSlots().stream().mapToInt(slot -> slot.index).toArray();
    }

    private static CompoundTag stationState(TinkerStationUpgradeContainer station) {
        // The host synchronizes menu slots separately from the upgrade item's persisted inventory.
        var state = new CompoundTag();
        state.putString("item", BuiltInRegistries.ITEM.getKey(station.getUpgradeStack().getItem()).toString());
        state.putString("layout", station.getUpgradeWrapper().layout().getName().toString());
        var materials = new ListTag();
        for (Slot slot : station.materialSlots()) materials.add(slot.getItem().save(new CompoundTag()));
        state.put("materials", materials);
        return state;
    }

    private static boolean matchesRecipe(Player player, ResourceLocation id, boolean modifier, List<ItemStack> selected) {
        var level = player.level();
        var recipe = level.getRecipeManager().byKey(id).orElse(null);
        if (recipe == null || recipe.getType() != TinkerRecipeTypes.TINKER_STATION.get()) return false;
        if (modifier) {
            return RecipeHelper.getJEIRecipes(level.registryAccess(), Stream.of(recipe), IDisplayModifierRecipe.class)
                    .stream().anyMatch(display -> matchesInputs(selected, display.getInputCount(), display::getDisplayItems));
        }
        if (!(recipe instanceof ToolBuildingRecipe building)) return false;
        var parts = building.getToolParts();
        var extras = building.getExtraRequirements();
        int required = parts.size() + extras.size();
        if (required > selected.size()) return false;
        for (int i = 0; i < selected.size(); i++) {
            ItemStack stack = selected.get(i);
            if (i < parts.size()) {
                var part = parts.get(i);
                if (stack.getCount() != 1 || !stack.is(part.asItem()) || !part.canUseMaterial(part.getMaterial(stack).getId())) return false;
            } else if (i < required) {
                var ingredient = extras.get(i - parts.size());
                if (stack.isEmpty() || !ingredient.test(stack) || matchingCount(stack, Arrays.asList(ingredient.getItems()), false) != stack.getCount()) return false;
            } else if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesInputs(List<ItemStack> selected, int inputCount, IntFunction<List<ItemStack>> inputs) {
        for (int i = 0; i < Math.max(inputCount, selected.size()); i++) {
            List<ItemStack> options = i < inputCount ? inputs.apply(i) : List.of();
            ItemStack stack = i < selected.size() ? selected.get(i) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                if (options.stream().anyMatch(option -> !option.isEmpty())) return false;
            } else if (matchingCount(stack, options, false) != stack.getCount()) {
                return false;
            }
        }
        return true;
    }

    public static Result preview(StorageContainerMenuBase<?> menu, TinkerStationUpgradeContainer station,
            List<List<ItemStack>> inputs, boolean maxTransfer) {
        return plan(menu, station, inputs, 1, maxTransfer ? 64 : 1, false);
    }

    private static Result plan(StorageContainerMenuBase<?> menu, TinkerStationUpgradeContainer station,
            List<List<ItemStack>> inputs, int minSets, int maxSets, boolean exact) {
        List<Slot> targets = station.materialSlots();
        int capacity = targets.size();
        if (inputs.stream().skip(capacity).anyMatch(options -> !options.isEmpty())) return new Result(Error.TOO_LARGE, null);
        if (inputs.stream().allMatch(List::isEmpty)) return new Result(Error.MISSING, null);

        Player player = station.getPlayer();
        List<Slot> inventory = new ArrayList<>();
        for (int i = 0; i < menu.getInventorySlotsSize(); i++) {
            Slot slot = menu.getSlot(i);
            boolean storage = menu.isStorageInventorySlot(i) && slot instanceof StorageInventorySlot storageSlot
                    && !storageSlot.isInfinite() && !menu.isInfiniteSlot(i) && !menu.isInaccessibleSlot(i);
            boolean playerInventory = slot.container == player.getInventory() && slot.getSlotIndex() >= 0 && slot.getSlotIndex() < 36;
            if ((storage || playerInventory) && slot.isActive() && canClear(slot, player)) inventory.add(slot);
        }
        for (Slot slot : targets) {
            if (menu.getSlot(slot.index) != slot || !slot.isActive() || !canClear(slot, player)) return new Result(Error.LAYOUT, null);
        }

        Map<Item, List<Supply>> supplies = new HashMap<>();
        for (var options : inputs) {
            for (ItemStack option : options) supplies.computeIfAbsent(option.getItem(), item -> new ArrayList<>());
        }
        for (Slot slot : targets) addSupply(supplies, slot.getItem());
        for (Slot slot : inventory) addSupply(supplies, slot.getItem());
        List<List<Choice>> choices = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            Slot target = targets.get(i);
            List<ItemStack> options = i < inputs.size() ? inputs.get(i) : List.of();
            if (options.isEmpty()) {
                choices.add(List.of());
                continue;
            }
            if (options.stream().noneMatch(target::mayPlace)) return new Result(Error.LAYOUT, null);
            List<Choice> available = new ArrayList<>();
            for (Item item : options.stream().map(ItemStack::getItem).distinct().toList()) {
                for (Supply supply : supplies.get(item)) {
                    int count = matchingCount(supply.stack, options, exact);
                    if (count == 0 || !target.mayPlace(supply.stack)) continue;
                    var choice = new Choice(supply, count, Math.min(target.getMaxStackSize(), supply.stack.getMaxStackSize()));
                    if (ItemStack.isSameItemSameTags(target.getItem(), supply.stack)) available.add(0, choice);
                    else available.add(choice);
                }
            }
            int upperBound = available.stream().mapToInt(Choice::maxSets).max().orElse(0);
            if (upperBound < minSets) return new Result(Error.MISSING, null);
            maxSets = Math.min(maxSets, upperBound);
            choices.add(available);
        }

        var search = new Search(targets, inventory, choices);
        for (int sets = maxSets; sets >= minSets; sets--) {
            Plan plan = search.select(0, sets);
            if (plan != null) return new Result(null, plan);
        }
        return new Result(search.hasMaterials ? Error.NO_ROOM : Error.MISSING, null);
    }

    private static boolean canClear(Slot slot, Player player) {
        // Forge's handler slots report mayPickup=false when empty.
        return !slot.hasItem() || slot.mayPickup(player);
    }

    private static int matchingCount(ItemStack stack, List<ItemStack> options, boolean exact) {
        for (ItemStack option : options) {
            if (stack.is(option.getItem()) && (ItemStack.isSameItemSameTags(stack, option) || !exact && !option.hasTag())) {
                return option.getCount();
            }
        }
        return 0;
    }

    private static void addSupply(Map<Item, List<Supply>> supplies, ItemStack stack) {
        if (stack.isEmpty()) return;
        var variants = supplies.get(stack.getItem());
        if (variants == null) return;
        for (Supply supply : variants) {
            if (ItemStack.isSameItemSameTags(supply.stack, stack)) {
                supply.count += stack.getCount();
                return;
            }
        }
        variants.add(new Supply(stack, stack.getCount()));
    }

    private static final class Supply {
        private final ItemStack stack;
        private long count;

        private Supply(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }

    private record Choice(Supply supply, int count, int limit) {
        int maxSets() { return (int) Math.min(limit, supply.count) / count; }
    }

    private static final class Search {
        private final List<Slot> targets;
        private final List<Slot> inventory;
        private final List<List<Choice>> choices;
        private final List<ItemStack> selected;
        private boolean hasMaterials;

        private Search(List<Slot> targets, List<Slot> inventory, List<List<Choice>> choices) {
            this.targets = targets;
            this.inventory = inventory;
            this.choices = choices;
            selected = new ArrayList<>(Collections.nCopies(targets.size(), ItemStack.EMPTY));
        }

        private Plan select(int position, int sets) {
            if (position == targets.size()) {
                hasMaterials = true;
                return arrange(sets);
            }
            List<Choice> options = choices.get(position);
            if (options.isEmpty()) return select(position + 1, sets);
            for (Choice choice : options) {
                if (choice.maxSets() < sets) continue;
                int count = sets * choice.count;
                choice.supply.count -= count;
                selected.set(position, choice.supply.stack.copyWithCount(choice.count));
                Plan plan = select(position + 1, sets);
                choice.supply.count += count;
                if (plan != null) return plan;
            }
            return null;
        }

        private Plan arrange(int sets) {
            List<ItemStack> oldMaterials = targets.stream().map(slot -> slot.getItem().copy()).toList();
            Map<Slot, ItemStack> remaining = new LinkedHashMap<>();
            for (Slot slot : inventory) remaining.put(slot, slot.getItem());

            // First reserve materials already in their correct positions, then draw from the rest of the grid and inventory.
            int[] needed = new int[targets.size()];
            for (int i = 0; i < needed.length; i++) {
                ItemStack stack = selected.get(i);
                needed[i] = take(oldMaterials.get(i), stack, stack.getCount() * sets);
            }
            for (int i = 0; i < needed.length; i++) {
                ItemStack stack = selected.get(i);
                int count = needed[i];
                for (ItemStack old : oldMaterials) count = take(old, stack, count);
                for (var entry : remaining.entrySet()) {
                    if (count == 0) break;
                    ItemStack available = entry.getValue();
                    if (!ItemStack.isSameItemSameTags(available, stack)) continue;
                    int taken = Math.min(available.getCount(), count);
                    entry.setValue(available.copyWithCount(available.getCount() - taken));
                    count -= taken;
                }
            }

            for (ItemStack old : oldMaterials) {
                if (!old.isEmpty()) {
                    returnToInventory(old, remaining, false);
                    returnToInventory(old, remaining, true);
                    if (!old.isEmpty()) return null;
                }
            }
            for (int i = 0; i < targets.size(); i++) {
                ItemStack stack = selected.get(i);
                remaining.put(targets.get(i), stack.copyWithCount(stack.getCount() * sets));
            }
            remaining.entrySet().removeIf(entry -> ItemStack.matches(entry.getKey().getItem(), entry.getValue()));
            return new Plan(sets, List.copyOf(selected), remaining);
        }
    }

    private static int take(ItemStack from, ItemStack ingredient, int count) {
        if (count == 0 || !ItemStack.isSameItemSameTags(from, ingredient)) return count;
        int taken = Math.min(from.getCount(), count);
        from.shrink(taken);
        return count - taken;
    }

    private static void returnToInventory(ItemStack stack, Map<Slot, ItemStack> inventory, boolean emptySlots) {
        for (var entry : inventory.entrySet()) {
            if (stack.isEmpty()) break;
            Slot slot = entry.getKey();
            ItemStack existing = entry.getValue();
            if (emptySlots != existing.isEmpty() || !emptySlots && !ItemStack.isSameItemSameTags(existing, stack)
                    || !slot.mayPlace(stack)) continue;
            int count = Math.min(stack.getCount(), slot.getMaxStackSize(stack) - existing.getCount());
            if (count > 0) {
                entry.setValue(stack.copyWithCount(existing.getCount() + count));
                stack.shrink(count);
            }
        }
    }
}
