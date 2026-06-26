package com.iamkaf.konfig.api.v1;

/**
 * Mutable view of a config file during schema migration.
 */
public interface ConfigMigrationContext {
    /**
     * Returns the owning mod id.
     *
     * @return the owning mod id
     */
    String modId();

    /**
     * Returns the config name being migrated.
     *
     * @return the config name
     */
    String name();

    /**
     * Returns the schema version the stored file was read as.
     *
     * @return the source schema version
     */
    int fromVersion();

    /**
     * Returns the target schema version.
     *
     * @return the target schema version
     */
    int toVersion();

    /**
     * Checks whether a path exists.
     *
     * @param path the dotted config path
     * @return true when the path exists
     */
    boolean contains(String path);

    /**
     * Reads a raw node.
     *
     * @param path the dotted config path
     * @return the node, or null when missing
     */
    KonfigNode node(String path);

    /**
     * Reads a boolean value.
     *
     * @param path the dotted config path
     * @return the value, or null when missing or incompatible
     */
    Boolean bool(String path);

    /**
     * Reads an integer value.
     *
     * @param path the dotted config path
     * @return the value, or null when missing or incompatible
     */
    Integer intValue(String path);

    /**
     * Reads a long value.
     *
     * @param path the dotted config path
     * @return the value, or null when missing or incompatible
     */
    Long longValue(String path);

    /**
     * Reads a double value.
     *
     * @param path the dotted config path
     * @return the value, or null when missing or incompatible
     */
    Double doubleValue(String path);

    /**
     * Reads a string value.
     *
     * @param path the dotted config path
     * @return the value, or null when missing or incompatible
     */
    String string(String path);

    /**
     * Writes a boolean value.
     *
     * @param path the dotted config path
     * @param value the value to write
     * @return this context
     */
    ConfigMigrationContext set(String path, boolean value);

    /**
     * Writes an integer value.
     *
     * @param path the dotted config path
     * @param value the value to write
     * @return this context
     */
    ConfigMigrationContext set(String path, int value);

    /**
     * Writes a long value.
     *
     * @param path the dotted config path
     * @param value the value to write
     * @return this context
     */
    ConfigMigrationContext set(String path, long value);

    /**
     * Writes a double value.
     *
     * @param path the dotted config path
     * @param value the value to write
     * @return this context
     */
    ConfigMigrationContext set(String path, double value);

    /**
     * Writes a string value.
     *
     * @param path the dotted config path
     * @param value the value to write
     * @return this context
     */
    ConfigMigrationContext set(String path, String value);

    /**
     * Writes a raw node.
     *
     * @param path the dotted config path
     * @param value the node to write
     * @return this context
     */
    ConfigMigrationContext set(String path, KonfigNode value);

    /**
     * Removes a path.
     *
     * @param path the dotted config path
     * @return true when a value was removed
     */
    boolean remove(String path);

    /**
     * Moves a value to a new path.
     *
     * @param fromPath the source dotted path
     * @param toPath the destination dotted path
     * @return true when a value was moved
     */
    boolean rename(String fromPath, String toPath);

    /**
     * Copies a value to a new path.
     *
     * @param fromPath the source dotted path
     * @param toPath the destination dotted path
     * @return true when a value was copied
     */
    boolean copy(String fromPath, String toPath);
}
