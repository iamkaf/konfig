package com.iamkaf.konfig.api.v1;

/**
 * Converts custom config values to and from raw Konfig nodes.
 *
 * @param <T> the custom value type
 */
public interface KonfigCodec<T> {
    /**
     * Decodes a raw node.
     *
     * @param node the raw node
     * @return the decoded value
     * @throws Exception when the node cannot be decoded
     */
    T decode(KonfigNode node) throws Exception;

    /**
     * Encodes a value into a raw node.
     *
     * @param value the value to encode
     * @return the encoded node
     * @throws Exception when the value cannot be encoded
     */
    KonfigNode encode(T value) throws Exception;
}
