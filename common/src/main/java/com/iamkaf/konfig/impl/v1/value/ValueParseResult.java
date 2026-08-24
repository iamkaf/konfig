//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.value;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.state.ConfigValidation;

import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
public sealed interface ValueParseResult<T> permits ValueParseResult.Parsed, ValueParseResult.Rejected {
    Optional<T> value();

    ConfigValidation validation();

    record Parsed<T>(T parsedValue, ConfigValidation validation) implements ValueParseResult<T> {
        public Parsed {
            parsedValue = Objects.requireNonNull(parsedValue, "parsedValue");
            validation = Objects.requireNonNull(validation, "validation");
            if (validation.hasErrors()) {
                throw new IllegalArgumentException("A parsed value cannot contain validation errors");
            }
        }

        @Override
        public Optional<T> value() {
            return Optional.of(this.parsedValue);
        }
    }

    record Rejected<T>(ConfigValidation validation) implements ValueParseResult<T> {
        public Rejected {
            validation = Objects.requireNonNull(validation, "validation");
            if (!validation.hasErrors()) {
                throw new IllegalArgumentException("A rejected value must contain a validation error");
            }
        }

        @Override
        public Optional<T> value() {
            return Optional.empty();
        }
    }
}
//?}
