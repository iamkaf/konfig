//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.value;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigFieldKind;
import com.iamkaf.konfig.impl.v1.state.ConfigValidation;

@ApiStatus.Internal
public interface ValueSemantics<T> {
    ConfigFieldKind kind();

    T copy(T value);

    T normalize(T value);

    ConfigValidation validate(String path, T value);

    ValueParseResult<T> parse(String path, Object input);
}
//?}
