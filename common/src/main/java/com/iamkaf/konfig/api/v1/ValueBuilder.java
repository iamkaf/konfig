package com.iamkaf.konfig.api.v1;

import java.util.function.Predicate;

public interface ValueBuilder<T> {
    ValueBuilder<T> comment(String comment);

    ValueBuilder<T> restart(RestartRequirement requirement);

    ValueBuilder<T> sync(boolean sync);

    ValueBuilder<T> clientOnly();

    ValueBuilder<T> serverOnly();

    ValueBuilder<T> validate(Predicate<T> validator, String errorMessage);

    ConfigValue<T> build();
}
