//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApiStatus.Internal
public final class ConfigCodecRegistry {
    private final Map<String, ConfigValueCodec<?>> codecs = new LinkedHashMap<>();

    public static ConfigCodecRegistry withBuiltIns() {
        var registry = new ConfigCodecRegistry();
        registry.register("boolean", scalar(JsonElement::getAsBoolean, JsonPrimitive::new));
        registry.register("integer", scalar(JsonElement::getAsInt, JsonPrimitive::new));
        registry.register("long", scalar(JsonElement::getAsLong, JsonPrimitive::new));
        registry.register("double", scalar(JsonElement::getAsDouble, JsonPrimitive::new));
        registry.register("string", scalar(JsonElement::getAsString, JsonPrimitive::new));
        registry.register("string_list", new ConfigValueCodec<List<String>>() {
            @Override
            public List<String> decode(JsonElement value) {
                if (!value.isJsonArray()) {
                    throw new IllegalArgumentException("Expected a string list");
                }
                var decoded = new ArrayList<String>();
                for (JsonElement element : value.getAsJsonArray()) {
                    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                        throw new IllegalArgumentException("Expected a string list");
                    }
                    decoded.add(element.getAsString());
                }
                return List.copyOf(decoded);
            }

            @Override
            public JsonElement encode(List<String> value) {
                var encoded = new JsonArray();
                value.forEach(encoded::add);
                return encoded;
            }
        });
        return registry;
    }

    public synchronized <T> void register(String id, ConfigValueCodec<T> codec) {
        String normalizedId = requireId(id);
        Objects.requireNonNull(codec, "codec");
        if (this.codecs.putIfAbsent(normalizedId, codec) != null) {
            throw new IllegalArgumentException("Duplicate config codec '" + normalizedId + "'");
        }
    }

    public synchronized ConfigValueCodec<?> require(String id) {
        ConfigValueCodec<?> codec = this.codecs.get(requireId(id));
        if (codec == null) {
            throw new IllegalArgumentException("Unknown config codec '" + id + "'");
        }
        return codec;
    }

    public synchronized <T> ConfigValueCodec<T> require(String id, Class<T> valueType) {
        Objects.requireNonNull(valueType, "valueType");
        return new TypeCheckingCodec<>(require(id), valueType, id);
    }

    public synchronized Map<String, ConfigValueCodec<?>> codecs() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.codecs));
    }

    private static <T> ConfigValueCodec<T> scalar(
            Decoder<T> decoder,
            java.util.function.Function<T, JsonElement> encoder
    ) {
        return new ConfigValueCodec<>() {
            @Override
            public T decode(JsonElement value) throws Exception {
                return decoder.decode(value);
            }

            @Override
            public JsonElement encode(T value) {
                return encoder.apply(value);
            }
        };
    }

    private static String requireId(String id) {
        String normalized = Objects.requireNonNull(id, "id").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("codec id cannot be blank");
        }
        return normalized;
    }

    @FunctionalInterface
    private interface Decoder<T> {
        T decode(JsonElement value) throws Exception;
    }

    private record TypeCheckingCodec<T>(ConfigValueCodec<?> delegate, Class<T> valueType, String codecId)
            implements ConfigValueCodec<T> {
        @Override
        public T decode(JsonElement value) throws Exception {
            Object decoded = this.delegate.decode(value);
            if (!this.valueType.isInstance(decoded)) {
                throw new IllegalArgumentException(
                        "Codec '" + this.codecId + "' decoded " + decoded.getClass().getName()
                                + " instead of " + this.valueType.getName()
                );
            }
            return this.valueType.cast(decoded);
        }

        @SuppressWarnings("unchecked")
        @Override
        public JsonElement encode(T value) throws Exception {
            return ((ConfigValueCodec<T>) this.delegate).encode(this.valueType.cast(value));
        }
    }
}
//?}
