package com.iamkaf.konfig.impl.v1;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class PathToml {
    private PathToml() {
    }

    static CommentedConfig read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CommentedConfig root = TomlFormat.newConfig();
            new TomlParser().parse(reader, root, ParsingMode.REPLACE);
            return root;
        }
    }

    static void write(Path path, CommentedConfig root, String fileComment) throws IOException {
        StringWriter writer = new StringWriter();
        new TomlWriter().write(root, writer);

        String content = writer.toString();
        String header = renderHeaderComment(fileComment);
        if (!header.isEmpty()) {
            content = header + System.lineSeparator() + content;
        }

        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    static JsonElement get(UnmodifiableConfig root, String dottedPath) {
        return toJson(root.getRaw(dottedPath), dottedPath);
    }

    static void put(CommentedConfig root, String dottedPath, JsonElement value) {
        root.set(dottedPath, toTomlValue(value, dottedPath));
    }

    static void setComment(CommentedConfig root, String dottedPath, String comment) {
        if (!isBlank(comment)) {
            root.setComment(dottedPath, comment.trim());
        }
    }

    private static Object toTomlValue(JsonElement element, String path) {
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("TOML does not support null values at " + path);
        }
        if (element.isJsonObject()) {
            CommentedConfig config = TomlFormat.newConfig();
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                config.set(entry.getKey(), toTomlValue(entry.getValue(), appendPath(path, entry.getKey())));
            }
            return config;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<Object> values = new ArrayList<Object>(array.size());
            for (int i = 0; i < array.size(); i++) {
                values.add(toTomlValue(array.get(i), path + '[' + i + ']'));
            }
            return values;
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return Boolean.valueOf(primitive.getAsBoolean());
        }
        if (primitive.isString()) {
            return primitive.getAsString();
        }
        if (primitive.isNumber()) {
            return toTomlNumber(primitive, path);
        }
        throw new IllegalArgumentException("Unsupported TOML value at " + path + ": " + primitive);
    }

    private static Number toTomlNumber(JsonPrimitive primitive, String path) {
        String raw = primitive.getAsString();
        if (raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0) {
            double value = Double.parseDouble(raw);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException("Unsupported TOML floating point value at " + path + ": " + raw);
            }
            return Double.valueOf(value);
        }
        try {
            return Long.valueOf(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("TOML integer out of range at " + path + ": " + raw, e);
        }
    }

    private static JsonElement toJson(Object value, String path) {
        if (value == null) {
            return null;
        }
        if (value instanceof UnmodifiableConfig) {
            JsonObject object = new JsonObject();
            UnmodifiableConfig config = (UnmodifiableConfig) value;
            for (Map.Entry<String, Object> entry : config.valueMap().entrySet()) {
                object.add(entry.getKey(), nullableJson(toJson(entry.getValue(), appendPath(path, entry.getKey()))));
            }
            return object;
        }
        if (value instanceof Map<?, ?>) {
            JsonObject object = new JsonObject();
            Map<?, ?> map = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                object.add(String.valueOf(entry.getKey()), nullableJson(toJson(entry.getValue(), appendPath(path, String.valueOf(entry.getKey())))));
            }
            return object;
        }
        if (value instanceof List<?>) {
            JsonArray array = new JsonArray();
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                array.add(nullableJson(toJson(list.get(i), path + '[' + i + ']')));
            }
            return array;
        }
        if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        }
        if (value instanceof Number) {
            Number number = (Number) value;
            if (number instanceof BigDecimal) {
                return new JsonPrimitive((BigDecimal) number);
            }
            return new JsonPrimitive(number);
        }
        if (value instanceof Character) {
            return new JsonPrimitive(String.valueOf(value));
        }
        return new JsonPrimitive(String.valueOf(value));
    }

    private static JsonElement nullableJson(JsonElement element) {
        return element == null ? JsonNull.INSTANCE : element;
    }

    private static String renderHeaderComment(String comment) {
        if (isBlank(comment)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        String[] lines = comment.replace("\r", "").split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                builder.append('#');
            } else {
                builder.append("# ").append(line);
            }
            if (i + 1 < lines.length) {
                builder.append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    private static String appendPath(String prefix, String child) {
        if (isBlank(prefix)) {
            return child;
        }
        return prefix + '.' + child;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
