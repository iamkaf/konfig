package com.iamkaf.konfig.api.v1;

public interface KonfigCodec<T> {
    T decode(KonfigNode node) throws Exception;

    KonfigNode encode(T value) throws Exception;
}
