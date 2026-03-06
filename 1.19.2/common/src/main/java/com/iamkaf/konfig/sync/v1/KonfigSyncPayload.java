package com.iamkaf.konfig.sync.v1;

import net.minecraft.network.FriendlyByteBuf;

public record KonfigSyncPayload(String configId, String jsonPayload) {
    public static void encode(KonfigSyncPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUtf(payload.configId(), 256);
        buffer.writeUtf(payload.jsonPayload());
    }

    public static KonfigSyncPayload decode(FriendlyByteBuf buffer) {
        return new KonfigSyncPayload(buffer.readUtf(256), buffer.readUtf());
    }
}
