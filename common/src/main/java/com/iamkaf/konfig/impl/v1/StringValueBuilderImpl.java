package com.iamkaf.konfig.impl.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.iamkaf.konfig.api.v1.RestartRequirement;
import com.iamkaf.konfig.api.v1.StringValueBuilder;
//? if >=1.17 {
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
//?}

import java.util.function.Predicate;

final class StringValueBuilderImpl extends ValueBuilderImpl<String> implements StringValueBuilder {
    StringValueBuilderImpl(
            ConfigBuilderImpl owner,
            String path,
            String defaultValue
    ) {
        super(
                owner,
                path,
                defaultValue,
                EntryKind.STRING,
                JsonElement::getAsString,
                JsonPrimitive::new
        );
    }

    @Override
    public StringValueBuilder comment(String comment) {
        super.comment(comment);
        return this;
    }

    @Override
    public StringValueBuilder restart(RestartRequirement requirement) {
        super.restart(requirement);
        return this;
    }

    @Override
    public StringValueBuilder sync(boolean sync) {
        super.sync(sync);
        return this;
    }

    @Override
    public StringValueBuilder clientOnly() {
        super.clientOnly();
        return this;
    }

    @Override
    public StringValueBuilder serverOnly() {
        super.serverOnly();
        return this;
    }

    @Override
    public StringValueBuilder validate(Predicate<String> validator, String errorMessage) {
        super.validate(validator, errorMessage);
        return this;
    }

    @Override
//? if <=1.16.5 {
    public StringValueBuilder registry(String registryId) {
        super.bindRegistry(registryId);
//?} else {
    public StringValueBuilder registry(ResourceKey<? extends Registry<?>> registryKey) {
        super.bindRegistry(registryKey);
//?}
        return this;
    }
}
