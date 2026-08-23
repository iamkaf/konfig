//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import com.google.gson.JsonElement;

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
        values.forEach((path, value) -> copy.put(path, value.deepCopy()));
        return Collections.unmodifiableMap(copy);
    }
}
//?}
