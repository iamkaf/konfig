package com.iamkaf.konfig.impl.v1;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.iamkaf.konfig.api.v1.ConfigMigrationContext;
import com.iamkaf.konfig.api.v1.KonfigNode;

import java.util.Objects;

final class ConfigMigrationContextImpl implements ConfigMigrationContext {
    private final String modId;
    private final String name;
    private final int fromVersion;
    private final int toVersion;
    private final CommentedConfig root;

    ConfigMigrationContextImpl(String modId, String name, int fromVersion, int toVersion, CommentedConfig root) {
        this.modId = modId;
        this.name = name;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.root = root;
    }

    @Override
    public String modId() {
        return this.modId;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public int fromVersion() {
        return this.fromVersion;
    }

    @Override
    public int toVersion() {
        return this.toVersion;
    }

    @Override
    public boolean contains(String path) {
        return this.root.contains(requirePath(path, "path"));
    }

    @Override
    public KonfigNode node(String path) {
        JsonElement element = PathToml.get(this.root, requirePath(path, "path"));
        return element == null ? null : new KonfigNode(element);
    }

    @Override
    public Boolean bool(String path) {
        JsonPrimitive primitive = primitive(path, "boolean");
        if (primitive == null) {
            return null;
        }
        if (!primitive.isBoolean()) {
            throw typeError(path, "boolean", primitive);
        }
        return Boolean.valueOf(primitive.getAsBoolean());
    }

    @Override
    public Integer intValue(String path) {
        JsonPrimitive primitive = numberPrimitive(path, "integer");
        if (primitive == null) {
            return null;
        }
        long value = parseInteger(primitive, path);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalStateException("Expected integer at " + path + ", found out-of-range value " + primitive.getAsString());
        }
        return Integer.valueOf((int) value);
    }

    @Override
    public Long longValue(String path) {
        JsonPrimitive primitive = numberPrimitive(path, "long");
        if (primitive == null) {
            return null;
        }
        return Long.valueOf(parseInteger(primitive, path));
    }

    @Override
    public Double doubleValue(String path) {
        JsonPrimitive primitive = numberPrimitive(path, "double");
        if (primitive == null) {
            return null;
        }
        return Double.valueOf(primitive.getAsDouble());
    }

    @Override
    public String string(String path) {
        JsonPrimitive primitive = primitive(path, "string");
        if (primitive == null) {
            return null;
        }
        if (!primitive.isString()) {
            throw typeError(path, "string", primitive);
        }
        return primitive.getAsString();
    }

    @Override
    public ConfigMigrationContext set(String path, boolean value) {
        return setPrimitive(path, new JsonPrimitive(Boolean.valueOf(value)));
    }

    @Override
    public ConfigMigrationContext set(String path, int value) {
        return setPrimitive(path, new JsonPrimitive(Integer.valueOf(value)));
    }

    @Override
    public ConfigMigrationContext set(String path, long value) {
        return setPrimitive(path, new JsonPrimitive(Long.valueOf(value)));
    }

    @Override
    public ConfigMigrationContext set(String path, double value) {
        return setPrimitive(path, new JsonPrimitive(Double.valueOf(value)));
    }

    @Override
    public ConfigMigrationContext set(String path, String value) {
        Objects.requireNonNull(value, "value");
        return setPrimitive(path, new JsonPrimitive(value));
    }

    @Override
    public ConfigMigrationContext set(String path, KonfigNode value) {
        Objects.requireNonNull(value, "value");
        JsonElement json = Objects.requireNonNull(value.json(), "value.json()");
        if (json.isJsonNull()) {
            throw new IllegalArgumentException("TOML does not support null values at " + path);
        }
        PathToml.put(this.root, requirePath(path, "path"), json);
        return this;
    }

    @Override
    public boolean remove(String path) {
        String normalized = requirePath(path, "path");
        if (!this.root.contains(normalized)) {
            return false;
        }
        this.root.remove(normalized);
        return true;
    }

    @Override
    public boolean rename(String fromPath, String toPath) {
        String from = requirePath(fromPath, "fromPath");
        String to = requirePath(toPath, "toPath");
        if (from.equals(to)) {
            return this.root.contains(from);
        }

        JsonElement value = PathToml.get(this.root, from);
        if (value == null) {
            return false;
        }

        PathToml.put(this.root, to, value);
        this.root.remove(from);
        return true;
    }

    @Override
    public boolean copy(String fromPath, String toPath) {
        String from = requirePath(fromPath, "fromPath");
        String to = requirePath(toPath, "toPath");
        if (from.equals(to)) {
            return this.root.contains(from);
        }

        JsonElement value = PathToml.get(this.root, from);
        if (value == null) {
            return false;
        }

        PathToml.put(this.root, to, value);
        return true;
    }

    private ConfigMigrationContext setPrimitive(String path, JsonPrimitive value) {
        PathToml.put(this.root, requirePath(path, "path"), value);
        return this;
    }

    private JsonPrimitive primitive(String path, String expectedType) {
        String normalized = requirePath(path, "path");
        JsonElement element = PathToml.get(this.root, normalized);
        if (element == null) {
            return null;
        }
        if (!element.isJsonPrimitive()) {
            throw new IllegalStateException("Expected " + expectedType + " at " + normalized + ", found " + describe(element));
        }
        return element.getAsJsonPrimitive();
    }

    private JsonPrimitive numberPrimitive(String path, String expectedType) {
        JsonPrimitive primitive = primitive(path, expectedType);
        if (primitive == null) {
            return null;
        }
        if (!primitive.isNumber()) {
            throw typeError(path, expectedType, primitive);
        }
        return primitive;
    }

    private static long parseInteger(JsonPrimitive primitive, String path) {
        String raw = primitive.getAsString();
        if (raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0) {
            throw new IllegalStateException("Expected integer at " + path + ", found " + raw);
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Expected integer at " + path + ", found " + raw, e);
        }
    }

    private static IllegalStateException typeError(String path, String expectedType, JsonPrimitive primitive) {
        return new IllegalStateException("Expected " + expectedType + " at " + path + ", found " + describe(primitive));
    }

    private static String describe(JsonElement element) {
        if (element == null) {
            return "missing value";
        }
        if (element.isJsonObject()) {
            return "object";
        }
        if (element.isJsonArray()) {
            return "array";
        }
        if (!element.isJsonPrimitive()) {
            return "value";
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return "boolean";
        }
        if (primitive.isString()) {
            return "string";
        }
        if (primitive.isNumber()) {
            return "number";
        }
        return "primitive";
    }

    private static String requirePath(String path, String name) {
        return ConfigMigrationSupport.requireUserPath(path, name);
    }
}
