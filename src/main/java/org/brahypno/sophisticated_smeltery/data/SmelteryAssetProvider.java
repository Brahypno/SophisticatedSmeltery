package org.brahypno.sophisticated_smeltery.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.function.BiConsumer;
import net.minecraft.data.PackOutput;

final class SmelteryAssetProvider extends JsonDataProvider {
    private final boolean models;

    SmelteryAssetProvider(PackOutput output, boolean models) {
        super(output, models ? "item models" : "upgrade tags");
        this.models = models;
    }

    @Override protected void generate(BiConsumer<String, JsonObject> output) {
        for (String host : java.util.List.of("sophisticatedbackpacks", "sophisticatedstorage")) {
            String prefix = host.equals("sophisticatedstorage") ? "storage_" : "";
            JsonArray upgrades = new JsonArray();
            for (String upgrade : SmelteryData.UPGRADES) {
                String name = prefix + upgrade;
                if (models) output.accept("assets/sophisticated_smeltery/models/item/" + name,
                        object("loader", "sophisticated_smeltery:upgrade_base", "host", host));
                upgrades.add(object("id", "sophisticated_smeltery:" + name, "required", false));
            }
            if (!models) output.accept("data/" + host + "/tags/items/upgrade", object("replace", false, "values", upgrades));
        }
    }
}
