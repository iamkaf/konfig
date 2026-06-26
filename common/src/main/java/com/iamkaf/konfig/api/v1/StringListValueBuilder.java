package com.iamkaf.konfig.api.v1;

//? if >=1.17 {
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
//?}

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Consumer;

/**
 * Configures a list of string config values.
 */
public interface StringListValueBuilder extends ValueBuilder<List<String>> {
    /**
     * Adds a file comment for this value.
     *
     * @param comment the comment text
     * @return this builder
     */
    @Override
    StringListValueBuilder comment(String comment);

    /**
     * Adds generated-screen tooltip text for this value.
     *
     * @param tooltip the tooltip text
     * @return this builder
     */
    @Override
    StringListValueBuilder tooltip(String tooltip);

    /**
     * Adds information-panel content for this value.
     *
     * @param builder the information-panel builder callback
     * @return this builder
     */
    @Override
    StringListValueBuilder info(Consumer<InfoPanelBuilder> builder);

    /**
     * Marks when changes to this value fully apply.
     *
     * @param requirement the restart requirement
     * @return this builder
     */
    @Override
    StringListValueBuilder restart(RestartRequirement requirement);

    /**
     * Controls whether this value participates in synchronization.
     *
     * @param sync true to synchronize this value when the handle synchronizes
     * @return this builder
     */
    @Override
    StringListValueBuilder sync(boolean sync);

    /**
     * Marks this value as client-only.
     *
     * @return this builder
     */
    @Override
    StringListValueBuilder clientOnly();

    /**
     * Marks this value as server-only.
     *
     * @return this builder
     */
    @Override
    StringListValueBuilder serverOnly();

    /**
     * Adds a list validation rule.
     *
     * @param validator predicate that returns true for valid values
     * @param errorMessage message shown when validation fails
     * @return this builder
     */
    @Override
    StringListValueBuilder validate(Predicate<List<String>> validator, String errorMessage);

//? if <=1.16.5 {
    /**
     * Binds list entries to registry-id suggestions.
     *
     * @param registryId the registry id to suggest from
     * @return this builder
     */
    StringListValueBuilder registry(String registryId);
//?} else {
    /**
     * Binds list entries to registry-key suggestions.
     *
     * @param registryKey the registry key to suggest from
     * @return this builder
     */
    StringListValueBuilder registry(ResourceKey<? extends Registry<?>> registryKey);
//?}
}
