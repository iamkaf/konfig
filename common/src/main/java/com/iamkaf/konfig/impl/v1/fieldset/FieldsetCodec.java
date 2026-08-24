//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.fieldset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iamkaf.konfig.api.v1.KonfigCodec;
import com.iamkaf.konfig.api.v1.KonfigNode;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntry;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntryOwnership;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetField;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetSchema;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;
import org.jetbrains.annotations.ApiStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Flat, deterministic wire and persistence codec for fieldset values.
 */
@ApiStatus.Internal
public final class FieldsetCodec implements KonfigCodec<FieldsetValue> {
    private static final String ID_KEY = "_konfig_id";

    private final FieldsetValue defaultValue;
    private final FieldsetSchema schema;
    private final Set<String> allowedKeys;

    public FieldsetCodec(FieldsetValue defaultValue) {
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.schema = defaultValue.schema();
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        keys.add(ID_KEY);
        for (FieldsetField<?> field : this.schema.fields()) {
            keys.add(field.key());
        }
        this.allowedKeys = java.util.Collections.unmodifiableSet(keys);
    }

    @Override
    public FieldsetValue decode(KonfigNode node) {
        JsonElement json = Objects.requireNonNull(node, "node").json();
        if (json == null || !json.isJsonArray()) {
            throw new IllegalArgumentException("Fieldset value must be an array of entries");
        }

        JsonArray array = json.getAsJsonArray();
        ArrayList<FieldsetEntry> entries = new ArrayList<FieldsetEntry>();
        for (FieldsetEntry entry : this.defaultValue.entries()) {
            if (entry.ownership() == FieldsetEntryOwnership.BUILTIN) {
                entries.add(entry);
            }
        }
        for (int index = 0; index < array.size(); index++) {
            JsonElement element = array.get(index);
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Fieldset entry " + index + " must be an object");
            }
            entries.add(decodeEntry(element.getAsJsonObject(), index));
        }
        return FieldsetValue.of(this.schema, entries);
    }

    @Override
    public KonfigNode encode(FieldsetValue value) {
        Objects.requireNonNull(value, "value");
        if (value.schema() != this.schema) {
            throw new IllegalArgumentException("Fieldset value was created from a different schema");
        }

        JsonArray array = new JsonArray();
        for (FieldsetEntry entry : value.entries()) {
            if (entry.ownership() == FieldsetEntryOwnership.BUILTIN) {
                continue;
            }
            JsonObject encoded = new JsonObject();
            encoded.addProperty(ID_KEY, entry.identity());
            for (FieldsetField<?> field : this.schema.fields()) {
                encodeField(encoded, entry, field);
            }
            array.add(encoded);
        }
        return new KonfigNode(array);
    }

    private FieldsetEntry decodeEntry(JsonObject object, int entryIndex) {
        for (String key : object.keySet()) {
            if (!this.allowedKeys.contains(key)) {
                throw new IllegalArgumentException("Fieldset entry " + entryIndex + " has unknown field: " + key);
            }
        }
        JsonElement identityElement = object.get(ID_KEY);
        if (identityElement == null || !identityElement.isJsonPrimitive() || !identityElement.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Fieldset entry " + entryIndex + " is missing string " + ID_KEY);
        }
        String identity = identityElement.getAsString();
        FieldsetEntry entry = FieldsetEntry.user(identity);

        for (FieldsetField<?> field : this.schema.fields()) {
            JsonElement fieldElement = object.get(field.key());
            if (fieldElement == null) {
                continue;
            }
            Object decoded = decodeField(field, fieldElement, entryIndex);
            entry = entry.withScalar(field, decoded);
        }
        return entry;
    }

    private static Object decodeField(FieldsetField<?> field, JsonElement element, int entryIndex) {
        try {
            switch (field.kind()) {
                case BOOLEAN:
                    requirePrimitiveKind(element, field, "boolean", element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean());
                    return Boolean.valueOf(element.getAsBoolean());
                case INTEGER:
                    requireNumber(element, field);
                    return Integer.valueOf(new BigDecimal(element.getAsString()).intValueExact());
                case LONG:
                    requireNumber(element, field);
                    return Long.valueOf(new BigDecimal(element.getAsString()).longValueExact());
                case DOUBLE:
                    requireNumber(element, field);
                    double value = element.getAsDouble();
                    if (!Double.isFinite(value)) {
                        throw new IllegalArgumentException("non-finite number");
                    }
                    return Double.valueOf(value);
                case OPTIONAL_STRING:
                    requireString(element, field);
                    return Optional.of(element.getAsString());
                case STRING:
                case DROPDOWN:
                case REGISTRY_STRING:
                    requireString(element, field);
                    return element.getAsString();
                default:
                    throw new IllegalArgumentException("Unsupported fieldset field kind: " + field.kind());
            }
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid fieldset entry " + entryIndex + " field " + field.key() + ": " + exception.getMessage(),
                    exception
            );
        }
    }

    private static void encodeField(JsonObject object, FieldsetEntry entry, FieldsetField<?> field) {
        Object value = entryValue(entry, field);
        switch (field.kind()) {
            case OPTIONAL_STRING:
                Optional<?> optional = (Optional<?>) value;
                if (optional.isPresent()) {
                    object.addProperty(field.key(), (String) optional.get());
                }
                return;
            case BOOLEAN:
                object.addProperty(field.key(), (Boolean) value);
                return;
            case INTEGER:
                object.addProperty(field.key(), (Integer) value);
                return;
            case LONG:
                object.addProperty(field.key(), (Long) value);
                return;
            case DOUBLE:
                object.addProperty(field.key(), (Double) value);
                return;
            case STRING:
            case DROPDOWN:
            case REGISTRY_STRING:
                object.addProperty(field.key(), (String) value);
                return;
            default:
                throw new IllegalArgumentException("Unsupported fieldset field kind: " + field.kind());
        }
    }

    private static Object entryValue(FieldsetEntry entry, FieldsetField<?> field) {
        return capturedEntryValue(entry, field);
    }

    private static <T> T capturedEntryValue(FieldsetEntry entry, FieldsetField<T> field) {
        return entry.value(field);
    }

    private static void requireNumber(JsonElement element, FieldsetField<?> field) {
        requirePrimitiveKind(element, field, "number", element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber());
    }

    private static void requireString(JsonElement element, FieldsetField<?> field) {
        requirePrimitiveKind(element, field, "string", element.isJsonPrimitive() && element.getAsJsonPrimitive().isString());
    }

    private static void requirePrimitiveKind(
            JsonElement element,
            FieldsetField<?> field,
            String expected,
            boolean matches
    ) {
        if (!matches) {
            throw new IllegalArgumentException("field " + field.key() + " must be a " + expected + ", got " + element);
        }
    }
}
//?}
