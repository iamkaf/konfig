package com.iamkaf.konfig.api.v1;

import java.util.List;

public interface ConfigBuilder {
    ConfigBuilder scope(ConfigScope scope);

    ConfigBuilder syncMode(SyncMode mode);

    ConfigBuilder fileName(String fileName);

    ConfigBuilder schemaVersion(int version);

    ConfigBuilder migrate(int fromVersion, ConfigMigration migration);

    ConfigBuilder push(String category);

    ConfigBuilder pop();

    ConfigBuilder comment(String comment);

    ConfigBuilder categoryComment(String comment);

    ValueBuilder<Boolean> bool(String key, boolean defaultValue);

    ValueBuilder<Integer> intRange(String key, int defaultValue, int min, int max);

    ValueBuilder<Long> longRange(String key, long defaultValue, long min, long max);

    ValueBuilder<Double> doubleRange(String key, double defaultValue, double min, double max);

    ValueBuilder<String> string(String key, String defaultValue, int minLen, int maxLen);

    ValueBuilder<List<String>> stringList(String key, List<String> defaultValue);

    <E extends Enum<E>> ValueBuilder<E> enumValue(String key, E defaultValue);

    ValueBuilder<Integer> colorRgb(String key, int defaultValue);

    ValueBuilder<Integer> colorArgb(String key, int defaultValue);

    <T> ValueBuilder<T> custom(String key, T defaultValue, KonfigCodec<T> codec);

    ConfigHandle build();
}
