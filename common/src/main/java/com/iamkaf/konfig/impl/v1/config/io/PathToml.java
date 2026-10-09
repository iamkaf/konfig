package com.iamkaf.konfig.impl.v1.config.io;

import org.jetbrains.annotations.ApiStatus;

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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public final class PathToml {
    private PathToml() {
    }

    public static CommentedConfig read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CommentedConfig root = TomlFormat.newConfig();
            new TomlParser().parse(reader, root, ParsingMode.REPLACE);
            return root;
        }
    }

    public static void write(Path path, CommentedConfig root, String fileComment) throws IOException {
        stripHeaderCopies(root, fileComment);
        StringWriter writer = new StringWriter();
        new TomlWriter().write(root, writer);

        String content = normalizeCommentSpacing(writer.toString());
        String header = renderHeaderComment(fileComment);
        if (!header.isEmpty()) {
            content = header + System.lineSeparator() + content;
        }

        Path parent = path.getParent();
        if (parent == null) {
            throw new IOException("Config path has no parent: " + path);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, path.getFileName().toString() + '.', ".tmp");
        try {
            Files.write(temporary, content.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static Path preserveBroken(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent == null) {
            throw new IOException("Config path has no parent: " + path);
        }
        String fileName = path.getFileName().toString();
        int suffix = 0;
        Path backup;
        do {
            String ending = suffix == 0 ? ".broken" : ".broken." + suffix;
            backup = parent.resolve(fileName + ending);
            suffix++;
        } while (Files.exists(backup));
        try {
            Files.move(path, backup, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(path, backup);
        }
        return backup;
    }

    public static JsonElement get(UnmodifiableConfig root, String dottedPath) {
        return toJson(root.getRaw(dottedPath), dottedPath);
    }

    public static void put(CommentedConfig root, String dottedPath, JsonElement value) {
        root.set(dottedPath, toTomlValue(value, dottedPath));
    }

    public static void setComment(CommentedConfig root, String dottedPath, String comment) {
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

    /**
     * The TOML parser attaches the file header to whichever top-level entry follows it, and that entry
     * can move once the file is rewritten. Drop every leading copy of the header from top-level comments
     * so writing it again never accumulates, and files that already accumulated copies heal.
     */
    private static void stripHeaderCopies(CommentedConfig root, String fileComment) {
        if (isBlank(fileComment)) {
            return;
        }
        List<String> header = trimmedLines(fileComment);
        for (Map.Entry<String, String> entry : new ArrayList<Map.Entry<String, String>>(root.commentMap().entrySet())) {
            String comment = entry.getValue();
            if (comment == null) {
                continue;
            }
            List<String> lines = Arrays.asList(comment.split("\\R", -1));
            int start = 0;
            while (lines.size() - start >= header.size()
                    && header.equals(trimmedLines(lines.subList(start, start + header.size())))) {
                start += header.size();
            }
            if (start == 0) {
                continue;
            }
            List<String> key = Collections.singletonList(entry.getKey());
            if (start == lines.size()) {
                root.removeComment(key);
            } else {
                root.setComment(key, String.join("\n", lines.subList(start, lines.size())));
            }
        }
    }

    private static List<String> trimmedLines(String text) {
        return trimmedLines(Arrays.asList(text.split("\\R", -1)));
    }

    private static List<String> trimmedLines(List<String> lines) {
        List<String> trimmed = new ArrayList<String>(lines.size());
        for (String line : lines) {
            trimmed.add(line.trim());
        }
        return trimmed;
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

    private static String normalizeCommentSpacing(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(content.length() + 32);
        String lineSeparator = System.lineSeparator();
        String[] lines = content.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            builder.append(normalizeCommentLine(lines[i]));
            if (i + 1 < lines.length) {
                builder.append(lineSeparator);
            }
        }
        return builder.toString();
    }

    private static String normalizeCommentLine(String line) {
        if (line == null || line.isEmpty()) {
            return line == null ? "" : line;
        }

        int hashIndex = line.indexOf('#');
        if (hashIndex < 0) {
            return line;
        }

        for (int i = 0; i < hashIndex; i++) {
            char character = line.charAt(i);
            if (!Character.isWhitespace(character)) {
                return line;
            }
        }

        if (hashIndex + 1 >= line.length()) {
            return line;
        }

        char next = line.charAt(hashIndex + 1);
        if (Character.isWhitespace(next) || next == '#') {
            return line;
        }

        return line.substring(0, hashIndex + 1) + ' ' + line.substring(hashIndex + 1);
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
