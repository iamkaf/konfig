package com.iamkaf.konfig.api.v1;

/**
 * Migrates stored config data between schema versions.
 */
public interface ConfigMigration {
    /**
     * Applies this migration.
     *
     * @param context the migration context
     * @throws Exception when the migration cannot be completed
     */
    void migrate(ConfigMigrationContext context) throws Exception;
}
