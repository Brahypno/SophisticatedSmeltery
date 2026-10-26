package org.brahypno.sophisticated_smeltery.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/** Shared writer for resources with custom serializers and optional, unregistered item IDs. */
abstract class JsonDataProvider implements DataProvider {
    private final Path root;
    private final String name;

    JsonDataProvider(PackOutput output, String name) {
        root = output.getOutputFolder();
        this.name = name;
    }

    protected abstract void generate(java.util.function.BiConsumer<String, JsonObject> output);

    @Override public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        generate((path, json) -> writes.add(DataProvider.saveStable(cache, json, root.resolve(path + ".json"))));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override public String getName() { return "Sophisticated Smeltery " + name; }

    static JsonObject object(Object... fields) {
        JsonObject result = new JsonObject();
        for (int i = 0; i < fields.length; i += 2) result.add((String)fields[i], json(fields[i + 1]));
        return result;
    }

    static JsonArray array(Object... values) {
        JsonArray result = new JsonArray();
        for (Object value : values) result.add(json(value));
        return result;
    }

    private static JsonElement json(Object value) {
        if (value instanceof JsonElement element) return element;
        if (value instanceof Boolean bool) return new JsonPrimitive(bool);
        if (value instanceof Number number) return new JsonPrimitive(number);
        return new JsonPrimitive((String)value);
    }
}
