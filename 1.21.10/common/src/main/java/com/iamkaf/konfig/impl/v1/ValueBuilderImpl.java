package com.iamkaf.konfig.impl.v1;

import com.google.gson.JsonElement;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.RestartRequirement;
import com.iamkaf.konfig.api.v1.ValueBuilder;

import java.util.function.Function;
import java.util.function.Predicate;

final class ValueBuilderImpl<T> implements ValueBuilder<T> {
    private final ConfigBuilderImpl owner;
    private final String path;
    private final T defaultValue;
    private final EntryKind kind;
    private final Function<JsonElement, T> decoder;
    private final Function<T, JsonElement> encoder;

    private String comment = "";
    private RestartRequirement restartRequirement = RestartRequirement.NONE;
    private boolean sync;
    private boolean clientOnly;
    private boolean serverOnly;
    private Predicate<T> validator = value -> true;
    private String validationMessage = "Invalid value";

    ValueBuilderImpl(
            ConfigBuilderImpl owner,
            String path,
            T defaultValue,
            EntryKind kind,
            Function<JsonElement, T> decoder,
            Function<T, JsonElement> encoder
    ) {
        this.owner = owner;
        this.path = path;
        this.defaultValue = defaultValue;
        this.kind = kind;
        this.decoder = decoder;
        this.encoder = encoder;
    }

    @Override
    public ValueBuilder<T> comment(String comment) {
        this.comment = comment == null ? "" : comment;
        return this;
    }

    @Override
    public ValueBuilder<T> restart(RestartRequirement requirement) {
        this.restartRequirement = requirement == null ? RestartRequirement.NONE : requirement;
        return this;
    }

    @Override
    public ValueBuilder<T> sync(boolean sync) {
        this.sync = sync;
        return this;
    }

    @Override
    public ValueBuilder<T> clientOnly() {
        this.clientOnly = true;
        this.serverOnly = false;
        return this;
    }

    @Override
    public ValueBuilder<T> serverOnly() {
        this.serverOnly = true;
        this.clientOnly = false;
        return this;
    }

    @Override
    public ValueBuilder<T> validate(Predicate<T> validator, String errorMessage) {
        this.validator = validator == null ? value -> true : validator;
        this.validationMessage = errorMessage == null ? "Invalid value" : errorMessage;
        return this;
    }

    @Override
    public ConfigValue<T> build() {
        ConfigValueImpl<T> entry = new ConfigValueImpl<>(
                this.path,
                this.defaultValue,
                this.kind,
                this.decoder,
                this.encoder,
                this.validator,
                this.validationMessage,
                this.sync,
                this.clientOnly,
                this.serverOnly,
                this.restartRequirement
        );

        this.owner.addEntry(this.path, entry, this.comment);
        return entry;
    }
}
