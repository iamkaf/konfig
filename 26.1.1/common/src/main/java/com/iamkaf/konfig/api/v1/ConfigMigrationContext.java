package com.iamkaf.konfig.api.v1;

public interface ConfigMigrationContext {
    String modId();

    String name();

    int fromVersion();

    int toVersion();

    boolean contains(String path);

    KonfigNode node(String path);

    Boolean bool(String path);

    Integer intValue(String path);

    Long longValue(String path);

    Double doubleValue(String path);

    String string(String path);

    ConfigMigrationContext set(String path, boolean value);

    ConfigMigrationContext set(String path, int value);

    ConfigMigrationContext set(String path, long value);

    ConfigMigrationContext set(String path, double value);

    ConfigMigrationContext set(String path, String value);

    ConfigMigrationContext set(String path, KonfigNode value);

    boolean remove(String path);

    boolean rename(String fromPath, String toPath);

    boolean copy(String fromPath, String toPath);
}
