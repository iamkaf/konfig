//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.storage;

import org.jetbrains.annotations.ApiStatus;

import com.google.gson.JsonElement;

@ApiStatus.Internal
public interface ConfigValueCodec<T> {
    T decode(JsonElement value) throws Exception;

    JsonElement encode(T value) throws Exception;
}
//?}
