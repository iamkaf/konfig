package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
public record ConfigStorageDocument(
        int schemaVersion,
        Map<String, JsonElement> values,
        Map<String, String> comments,
        String fileComment
) {
    public ConfigStorageDocument {
        if (schemaVersion < 0) {
            throw new IllegalArgumentException("schemaVersion must be non-negative");
        }
        values = copyValues(values);
        comments = Collections.unmodifiableMap(new LinkedHashMap<>(comments));
        fileComment = fileComment == null ? "" : fileComment;
    }

    public ConfigStorageDocument withSchemaVersion(int version) {
        return new ConfigStorageDocument(version, this.values, this.comments, this.fileComment);
    }

    public ConfigStorageDocument copy() {
        return new ConfigStorageDocument(this.schemaVersion, this.values, this.comments, this.fileComment);
    }

    private static Map<String, JsonElement> copyValues(Map<String, JsonElement> values) {
        var copy = new LinkedHashMap<String, JsonElement>();
        values.forEach((path, value) -> copy.put(path, deepCopy(value)));
        return Collections.unmodifiableMap(copy);
    }

    // JsonElement.deepCopy() is package-private in Gson 2.8.0, which 1.17-1.17.1 ship.
    private static JsonElement deepCopy(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject copy = new JsonObject();
            for (Map.Entry<String, JsonElement> member : value.getAsJsonObject().entrySet()) {
                copy.add(member.getKey(), deepCopy(member.getValue()));
            }
            return copy;
        }
        if (value.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement element : value.getAsJsonArray()) {
                copy.add(deepCopy(element));
            }
            return copy;
        }
        // JsonPrimitive and JsonNull are immutable.
        return value;
    }
}
