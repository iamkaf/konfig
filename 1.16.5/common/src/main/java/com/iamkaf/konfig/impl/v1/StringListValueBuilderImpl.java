package com.iamkaf.konfig.impl.v1;

import com.iamkaf.konfig.api.v1.RestartRequirement;
import com.iamkaf.konfig.api.v1.StringListValueBuilder;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

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
    public StringListValueBuilder registry(String registryId) {
        super.bindRegistry(registryId);
        return this;
    }

    @Override
    StringListValueBuilderImpl canonicalize(UnaryOperator<List<String>> canonicalizer) {
        super.canonicalize(canonicalizer);
        return this;
    }
}
