package com.iamkaf.konfig.api.v1;

public interface ConfigMigration {
    void migrate(ConfigMigrationContext context) throws Exception;
}
