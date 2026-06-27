package com.iamkaf.konfig.impl.v1.config.builder;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.RestartRequirement;
import com.iamkaf.konfig.api.v1.StringListValueBuilder;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;
import com.iamkaf.konfig.impl.v1.config.model.StringListValueHelper;
//? if >=1.17 {
// Registry-bound string lists use ResourceKey on modern lines; <=1.16.5 keeps
// string registry ids to match the legacy Minecraft registry API.
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
//?}

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@ApiStatus.Internal
final class StringListValueBuilderImpl extends ValueBuilderImpl<List<String>> implements StringListValueBuilder {
    StringListValueBuilderImpl(
            ConfigBuilderImpl owner,
            String path,
            List<String> defaultValue
    ) {
        super(
                owner,
                path,
                defaultValue,
                EntryKind.STRING_LIST,
                json -> StringListValueHelper.decode(json, path),
                value -> StringListValueHelper.encode(value, path)
        );
    }

    @Override
    public StringListValueBuilder comment(String comment) {
        super.comment(comment);
        return this;
    }

    @Override
    public StringListValueBuilder tooltip(String tooltip) {
        super.tooltip(tooltip);
        return this;
    }

    @Override
    public StringListValueBuilder info(Consumer<com.iamkaf.konfig.api.v1.InfoPanelBuilder> builder) {
        super.info(builder);
        return this;
    }

    @Override
    public StringListValueBuilder restart(RestartRequirement requirement) {
        super.restart(requirement);
        return this;
    }

    @Override
    public StringListValueBuilder sync(boolean sync) {
        super.sync(sync);
        return this;
    }

    @Override
    public StringListValueBuilder clientOnly() {
        super.clientOnly();
        return this;
    }

    @Override
    public StringListValueBuilder serverOnly() {
        super.serverOnly();
        return this;
    }

    @Override
    public StringListValueBuilder validate(Predicate<List<String>> validator, String errorMessage) {
        super.validate(validator, errorMessage);
        return this;
    }

    @Override
//? if <=1.16.5 {
    public StringListValueBuilder registry(String registryId) {
        super.bindRegistry(registryId);
//?} else {
    public StringListValueBuilder registry(ResourceKey<? extends Registry<?>> registryKey) {
        super.bindRegistry(registryKey);
//?}
        return this;
    }

    @Override
    StringListValueBuilderImpl canonicalize(UnaryOperator<List<String>> canonicalizer) {
        super.canonicalize(canonicalizer);
        return this;
    }
}
