package com.iamkaf.konfig.api.v1;

import java.util.List;
import java.util.function.Predicate;

public interface StringListValueBuilder extends ValueBuilder<List<String>> {
    @Override
    StringListValueBuilder comment(String comment);

    @Override
    StringListValueBuilder restart(RestartRequirement requirement);

    @Override
    StringListValueBuilder sync(boolean sync);

    @Override
    StringListValueBuilder clientOnly();

    @Override
    StringListValueBuilder serverOnly();

    @Override
    StringListValueBuilder validate(Predicate<List<String>> validator, String errorMessage);

    StringListValueBuilder registry(String registryId);
}
