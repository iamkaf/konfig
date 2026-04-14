package com.iamkaf.konfig.impl.v1;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

final class ConfigMigrationSupport {
    static final String METADATA_ROOT = "__konfig";
    static final String VERSION_PATH = METADATA_ROOT + ".version";
    static final String METADATA_COMMENT = "Konfig internal metadata. Safe to ignore.";

    private ConfigMigrationSupport() {
    }

    static void validateSchemaVersion(int version) {
        if (version < 0) {
            throw new IllegalArgumentException("schemaVersion must be >= 0: " + version);
        }
    }

    static boolean isReservedRootSegment(String segment) {
        return METADATA_ROOT.equals(segment);
    }

    static String requireUserPath(String path, String name) {
        String normalized = path == null ? "" : path.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        if (normalized.indexOf('/') >= 0 || normalized.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(name + " contains unsupported characters: " + normalized);
        }

        String[] parts = normalized.split("\\.", -1);
        for (String part : parts) {
            if (part.trim().isEmpty()) {
                throw new IllegalArgumentException(name + " contains an empty path segment: " + normalized);
            }
            if (part.indexOf('/') >= 0 || part.indexOf('\\') >= 0) {
                throw new IllegalArgumentException(name + " contains unsupported characters: " + normalized);
            }
        }

        if (isReservedPath(normalized)) {
            throw new IllegalArgumentException(name + " uses reserved Konfig metadata path: " + normalized);
        }
        return normalized;
    }

    static int readSchemaVersion(CommentedConfig root) {
        JsonElement element = PathToml.get(root, VERSION_PATH);
        if (element == null) {
            return 0;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalStateException("Invalid Konfig schema version at " + VERSION_PATH + ": expected integer");
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        String raw = primitive.getAsString();
        if (raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0) {
            throw new IllegalStateException("Invalid Konfig schema version at " + VERSION_PATH + ": expected integer, found " + raw);
        }

        try {
            int version = Integer.parseInt(raw);
            validateSchemaVersion(version);
            return version;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid Konfig schema version at " + VERSION_PATH + ": " + raw, e);
        }
    }

    static void writeSchemaVersion(CommentedConfig root, int version) {
        validateSchemaVersion(version);
        PathToml.put(root, VERSION_PATH, new JsonPrimitive(Integer.valueOf(version)));
        if (root.contains(METADATA_ROOT)) {
            PathToml.setComment(root, METADATA_ROOT, METADATA_COMMENT);
        }
    }

    private static boolean isReservedPath(String path) {
        return METADATA_ROOT.equals(path) || path.startsWith(METADATA_ROOT + '.');
    }
}
