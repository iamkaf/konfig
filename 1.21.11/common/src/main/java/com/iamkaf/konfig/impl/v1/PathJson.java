package com.iamkaf.konfig.impl.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class PathJson {
    private PathJson() {
    }

    static JsonElement get(JsonObject root, String dottedPath) {
        String[] parts = dottedPath.split("\\.");
        JsonElement current = root;
        for (String part : parts) {
            if (!current.isJsonObject()) {
                return null;
            }
            JsonObject obj = current.getAsJsonObject();
            if (!obj.has(part)) {
                return null;
            }
            current = obj.get(part);
        }
        return current;
    }

    static void put(JsonObject root, String dottedPath, JsonElement value) {
        String[] parts = dottedPath.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            JsonObject next;
            if (!current.has(part) || !current.get(part).isJsonObject()) {
                next = new JsonObject();
                current.add(part, next);
            } else {
                next = current.getAsJsonObject(part);
            }
            current = next;
        }
        current.add(parts[parts.length - 1], value);
    }
}
