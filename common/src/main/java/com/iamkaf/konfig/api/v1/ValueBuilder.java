package com.iamkaf.konfig.api.v1;

import java.util.function.Predicate;
import java.util.function.Consumer;

/**
 * Configures one typed config value before the owning config is built.
 *
 * @param <T> the value type
 */
public interface ValueBuilder<T> {
    /**
     * Adds a file comment for this value.
     *
     * @param comment the comment text
     * @return this builder
     */
    ValueBuilder<T> comment(String comment);

    /**
     * Adds generated-screen tooltip text for this value.
     *
     * @param tooltip the tooltip text
     * @return this builder
     */
    ValueBuilder<T> tooltip(String tooltip);

    /**
     * Adds information-panel content for this value.
     *
     * @param builder the information-panel builder callback
     * @return this builder
     */
    ValueBuilder<T> info(Consumer<InfoPanelBuilder> builder);

    /**
     * Marks when changes to this value fully apply.
     *
     * @param requirement the restart requirement
     * @return this builder
     */
    ValueBuilder<T> restart(RestartRequirement requirement);

    /**
     * Controls whether this value participates in synchronization.
     *
     * @param sync true to synchronize this value when the handle synchronizes
     * @return this builder
     */
    ValueBuilder<T> sync(boolean sync);

    /**
     * Marks this value as client-only.
     *
     * @return this builder
     */
    ValueBuilder<T> clientOnly();

    /**
     * Marks this value as server-only.
     *
     * @return this builder
     */
    ValueBuilder<T> serverOnly();

    /**
     * Adds a validation rule.
     *
     * @param validator predicate that returns true for valid values
     * @param errorMessage message shown when validation fails
     * @return this builder
     */
    ValueBuilder<T> validate(Predicate<T> validator, String errorMessage);

    /**
     * Finalizes this entry and returns its runtime value handle.
     *
     * @return the config value handle
     */
    ConfigValue<T> build();
}
