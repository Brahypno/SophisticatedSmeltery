package org.brahypno.sophisticated_smeltery.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.data.PackOutput;

/** Generates recipes and their unlocks together, preserving the published IDs and optional-mod conditions. */
final class SmelteryRecipeProvider extends JsonDataProvider {
    private static final String MOD = "sophisticated_smeltery:";
    private BiConsumer<String, JsonObject> output;

    SmelteryRecipeProvider(PackOutput output) { super(output, "recipes and advancements"); }

    @Override protected void generate(BiConsumer<String, JsonObject> output) {
        this.output = output;
        recipe("workstation_appearance", object("type", MOD + "workstation_appearance"), null);
        for (String host : List.of("sophisticatedbackpacks", "sophisticatedstorage")) {
            String prefix = host.equals("sophisticatedstorage") ? "storage_" : "";
            JsonArray conditions = conditions(host);
            String base = host + ":upgrade_base";
            shaped(prefix + "melting_upgrade", "minecraft:crafting_shaped", array("BBB", "BUB", "BBB"),
                    object("B", item("tconstruct:seared_brick"), "U", item(base)), prefix + "melting_upgrade", conditions,
                    "has_upgrade_base", predicate(base));
            shaped(prefix + "part_builder_upgrade", "minecraft:crafting_shaped", array(" T ", "IBI", " C "),
                    object("T", item("tconstruct:part_builder"), "I", tag("forge:ingots/iron"),
                            "B", item(base), "C", tag("forge:chests")), prefix + "part_builder_upgrade", conditions,
                    "has_upgrade_base", predicate(base));
            shaped(prefix + "spawner_upgrade", "minecraft:crafting_shaped", array("BBB", "BUB", "BBB"),
                    object("B", item("minecraft:iron_bars"), "U", item(base)), prefix + "spawner_upgrade", conditions,
                    "has_bars", predicate("minecraft:iron_bars"));
            shaped(prefix + "modifier_worktable_upgrade", MOD + "casting_upgrade_tier", array(" T ", "IBI", " C "),
                    object("T", item("tconstruct:modifier_worktable"), "I", tag("forge:ingots/iron"),
                            "B", item(base), "C", tag("forge:chests")), prefix + "modifier_worktable_upgrade", conditions,
                    "has_upgrade_base", predicate(base));
            shaped(prefix + "tinker_station_upgrade", MOD + "casting_upgrade_tier", array(" T ", "IBI", " C "),
                    object("T", item("tconstruct:tinker_station"), "I", tag("forge:ingots/iron"),
                            "B", item(base), "C", tag("forge:chests")), prefix + "tinker_station_upgrade", conditions,
                    "has_upgrade_base", predicate(base));
            shaped(prefix + "tinkers_anvil_upgrade", MOD + "casting_upgrade_tier", array(" A ", "GUG", "RRR"),
                    object("A", array(item("tconstruct:tinkers_anvil"), item("tconstruct:scorched_anvil")), "G", tag("forge:ingots/gold"),
                            "U", item(MOD + prefix + "tinker_station_upgrade"), "R", tag("forge:dusts/redstone")),
                    prefix + "tinkers_anvil_upgrade", conditions, "has_upgrade", predicate(MOD + prefix + "tinker_station_upgrade"));
            recipe(prefix + "alloyer_upgrade", object("type", "minecraft:crafting_shapeless",
                    "ingredients", array(item(base), item("tconstruct:scorched_alloyer")),
                    "result", item(MOD + prefix + "scorched_alloyer_upgrade"), "conditions", conditions),
                    criteria("has_controller", predicate("tconstruct:scorched_alloyer")));
            for (String material : List.of("seared", "scorched")) {
                String casting = material + "_casting_upgrade";
                shaped(prefix + casting, "minecraft:crafting_shaped", array("BFT", "CUC", "FCF"),
                        object("B", item("tconstruct:" + material + "_basin"), "F", item("tconstruct:" + material + "_faucet"),
                                "T", item("tconstruct:" + material + "_table"), "C", item("tconstruct:" + material + "_channel"), "U", item(base)),
                        prefix + casting, conditions, "has_upgrade_base", predicate(base));
                advanced(prefix, casting, conditions, "has_casting_upgrade");
            }
            advanced(prefix, "spawner_upgrade", conditions, "has_upgrade");
            for (String controller : List.of("smeltery", "foundry", "transmute")) {
                boolean transmute = controller.equals("transmute");
                String controllerId = (transmute ? "esotericism_tinker:" : "tconstruct:") + controller + "_controller";
                JsonObject unlocks = transmute ? criteria("has_upgrade_base", predicate(base))
                        : criteria("has_upgrade", predicate(MOD + prefix + "melting_upgrade"), "has_controller", predicate(controllerId));
                recipe(prefix + controller + "_upgrade", object("type", MOD + "controller_binding", "controller", controllerId,
                        "upgrade", MOD + prefix + "melting_upgrade", "conditions", transmute ? conditions(host, "esotericism_tinker") : conditions), unlocks);
            }
            JsonObject transmuteInput = item(MOD + prefix + "melting_upgrade");
            transmuteInput.addProperty("type", "forge:partial_nbt");
            transmuteInput.addProperty("nbt", "{controller:{id:\"esotericism_tinker:transmute_controller\",Count:1b}}");
            JsonObject transmuteRecipe = object("type", MOD + "transmute_upgrade_tier", "pattern", array("ADM", "GUG", "RRR"),
                    "key", object("A", item("esotericism_tinker:ashen_alloy_switch"), "D", tag("forge:gems/diamond"),
                            "M", item("esotericism_tinker:ashen_melt_switch"), "G", tag("forge:ingots/gold"), "U", transmuteInput, "R", tag("forge:dusts/redstone")),
                    "result", item(MOD + prefix + "advanced_transmute_upgrade"), "conditions", conditions(host, "esotericism_tinker"));
            JsonObject transmutePredicate = predicate(MOD + prefix + "melting_upgrade");
            transmutePredicate.addProperty("nbt", "{controller:{id:\"esotericism_tinker:transmute_controller\"}}");
            recipe(prefix + "advanced_transmute_upgrade", transmuteRecipe, criteria("has_transmute_upgrade", transmutePredicate));
        }
        for (String upgrade : SmelteryData.UPGRADES) {
            JsonArray conditions = upgrade.equals("advanced_transmute_upgrade")
                    ? conditions("sophisticatedbackpacks", "sophisticatedstorage", "esotericism_tinker")
                    : conditions("sophisticatedbackpacks", "sophisticatedstorage");
            shaped("storage_" + upgrade + "_from_backpack_" + upgrade, MOD + "casting_upgrade_tier", array("PUP", " P ", "P P"),
                    object("P", tag("minecraft:planks"), "U", item(MOD + upgrade)), "storage_" + upgrade, conditions,
                    "has_source_upgrade", predicate(MOD + upgrade));
            shaped("backpack_" + upgrade + "_from_storage_" + upgrade, MOD + "casting_upgrade_tier", array("TUT", " L ", "T T"),
                    object("T", tag("forge:string"), "L", tag("forge:leather"), "U", item(MOD + "storage_" + upgrade)), upgrade, conditions,
                    "has_source_upgrade", predicate(MOD + "storage_" + upgrade));
        }
    }

    private void advanced(String prefix, String upgrade, JsonArray conditions, String criterion) {
        shaped(prefix + "advanced_" + upgrade, MOD + "casting_upgrade_tier", array(" D ", "GUG", "RRR"),
                object("D", tag("forge:gems/diamond"), "G", tag("forge:ingots/gold"), "U", item(MOD + prefix + upgrade), "R", tag("forge:dusts/redstone")),
                prefix + "advanced_" + upgrade, conditions, criterion, predicate(MOD + prefix + upgrade));
    }

    private void shaped(String name, String type, JsonArray pattern, JsonObject key, String result, JsonArray conditions, String criterion, JsonObject predicate) {
        recipe(name, object("type", type, "category", "misc", "pattern", pattern, "key", key, "result", item(MOD + result), "conditions", conditions),
                criteria(criterion, predicate));
    }

    private void recipe(String name, JsonObject recipe, JsonObject criteria) {
        output.accept("data/sophisticated_smeltery/recipes/" + name, recipe);
        if (criteria == null) return;
        JsonArray requirement = new JsonArray();
        criteria.keySet().forEach(requirement::add);
        // The original controller/alloyer unlocks only depend on inventory criteria.
        boolean inventoryUnlock = name.endsWith("alloyer_upgrade") && !name.contains("_from_")
                || recipe.get("type").getAsString().equals(MOD + "controller_binding");
        if (!inventoryUnlock) {
            requirement.add("has_the_recipe");
            criteria.add("has_the_recipe", object("trigger", "minecraft:recipe_unlocked", "conditions", object("recipe", MOD + name)));
        }
        JsonObject advancement = object("parent", "minecraft:recipes/root", "criteria", criteria,
                "requirements", array(requirement), "rewards", object("recipes", array(MOD + name)), "conditions", recipe.get("conditions"));
        if (recipe.get("type").getAsString().equals(MOD + "controller_binding") && name.endsWith("transmute_upgrade"))
            advancement.remove("requirements");
        String folder = name.contains("spawner_upgrade") || name.contains("_from_") ? "recipes/misc/" : "recipes/";
        output.accept("data/sophisticated_smeltery/advancements/" + folder + name, advancement);
    }

    private static JsonObject item(String id) { return object("item", id); }
    private static JsonObject tag(String id) { return object("tag", id); }
    private static JsonObject predicate(String id) { return object("items", array(id)); }
    private static JsonObject criteria(Object... predicates) {
        JsonObject result = new JsonObject();
        for (int i = 0; i < predicates.length; i += 2) result.add((String)predicates[i],
                object("trigger", "minecraft:inventory_changed", "conditions", object("items", array(predicates[i + 1]))));
        return result;
    }
    private static JsonArray conditions(String... mods) {
        JsonArray result = new JsonArray();
        for (String mod : mods) result.add(object("type", "forge:mod_loaded", "modid", mod));
        return result;
    }
}
