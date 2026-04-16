package com.iamkaf.konfig.api.v1;

import com.google.gson.JsonElement;

public final class KonfigNode {
    private final JsonElement json;

    public KonfigNode(JsonElement json) {
        this.json = json;
    }

    public JsonElement json() {
        return this.json;
    }
}
