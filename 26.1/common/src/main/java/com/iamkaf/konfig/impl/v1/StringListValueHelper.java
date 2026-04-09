package com.iamkaf.konfig.impl.v1;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StringListValueHelper {
    private StringListValueHelper() {
    }

    static List<String> immutableCopy(List<String> values, String path) {
        if (values == null) {
            throw new IllegalArgumentException("List value cannot be null for '" + path + "'.");
        }

        List<String> copy = new ArrayList<String>(values.size());
        for (Object value : values) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("List value must contain only strings for '" + path + "'.");
            }
            copy.add((String) value);
        }
        return Collections.unmodifiableList(copy);
    }

    static List<String> mutableCopy(List<String> values) {
        return values == null ? new ArrayList<String>() : new ArrayList<String>(values);
    }

    static List<String> decode(JsonElement element, String path) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("Expected string list for '" + path + "'.");
        }

        JsonArray array = element.getAsJsonArray();
        List<String> values = new ArrayList<String>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonElement item = array.get(i);
            if (item == null || !item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("List value must contain only strings for '" + path + "'.");
            }
            values.add(item.getAsString());
        }
        return immutableCopy(values, path);
    }

    static JsonArray encode(List<String> values, String path) {
        List<String> copy = immutableCopy(values, path);
        JsonArray array = new JsonArray();
        for (String value : copy) {
            array.add(value);
        }
        return array;
    }
}
