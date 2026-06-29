package com.iamkaf.konfig.api.v1;

import com.google.gson.JsonElement;

/**
 * Raw JSON-like node used by custom codecs and migrations.
 */
public final class KonfigNode {
    private final JsonElement json;

    /**
     * Creates a node wrapper.
     *
     * @param json the backing JSON element
     */
    public KonfigNode(JsonElement json) {
        this.json = json;
    }

    /**
     * Returns the backing JSON element.
     *
     * @return the backing element
     */
    public JsonElement json() {
        return this.json;
    }
}
