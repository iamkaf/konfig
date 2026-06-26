package com.iamkaf.konfig.api.v1;

//? if >=1.17 {
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
//?}

import java.util.function.Predicate;
import java.util.function.Consumer;

/**
 * Configures a string config value.
 */
public interface StringValueBuilder extends ValueBuilder<String> {
    /**
     * Adds a file comment for this value.
     *
     * @param comment the comment text
     * @return this builder
     */
    @Override
    StringValueBuilder comment(String comment);

    /**
     * Adds generated-screen tooltip text for this value.
     *
     * @param tooltip the tooltip text
     * @return this builder
     */
    @Override
    StringValueBuilder tooltip(String tooltip);

    /**
     * Adds information-panel content for this value.
     *
     * @param builder the information-panel builder callback
     * @return this builder
     */
    @Override
    StringValueBuilder info(Consumer<InfoPanelBuilder> builder);

    /**
     * Marks when changes to this value fully apply.
     *
     * @param requirement the restart requirement
     * @return this builder
     */
    @Override
    StringValueBuilder restart(RestartRequirement requirement);

    /**
     * Controls whether this value participates in synchronization.
     *
     * @param sync true to synchronize this value when the handle synchronizes
     * @return this builder
     */
    @Override
    StringValueBuilder sync(boolean sync);

    /**
     * Marks this value as client-only.
     *
     * @return this builder
     */
    @Override
    StringValueBuilder clientOnly();

    /**
     * Marks this value as server-only.
     *
     * @return this builder
     */
    @Override
    StringValueBuilder serverOnly();

    /**
     * Adds a string validation rule.
     *
     * @param validator predicate that returns true for valid values
     * @param errorMessage message shown when validation fails
     * @return this builder
     */
    @Override
    StringValueBuilder validate(Predicate<String> validator, String errorMessage);

//? if <=1.16.5 {
    /**
     * Binds this value to registry-id suggestions.
     *
     * @param registryId the registry id to suggest from
     * @return this builder
     */
    StringValueBuilder registry(String registryId);
//?} else {
    /**
     * Binds this value to registry-key suggestions.
     *
     * @param registryKey the registry key to suggest from
     * @return this builder
     */
    StringValueBuilder registry(ResourceKey<? extends Registry<?>> registryKey);
//?}
}
