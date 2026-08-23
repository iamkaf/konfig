package com.iamkaf.konfig.impl.v1.sync;

//? if >=1.21.11 {
import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class KonfigRemotePayloads {
    private KonfigRemotePayloads() {
    }

    public record Hello(int protocolVersion) implements CustomPacketPayload {
        public static final Type<Hello> TYPE = new Type<>(Constants.resource("remote_hello"));
        public static final StreamCodec<FriendlyByteBuf, Hello> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeVarInt(payload.protocolVersion()),
                buffer -> new Hello(buffer.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Capabilities(int protocolVersion, boolean canEdit) implements CustomPacketPayload {
        public static final Type<Capabilities> TYPE = new Type<>(Constants.resource("remote_capabilities"));
        public static final StreamCodec<FriendlyByteBuf, Capabilities> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.protocolVersion());
                    buffer.writeBoolean(payload.canEdit());
                },
                buffer -> new Capabilities(buffer.readVarInt(), buffer.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Snapshot(String configId, long revision, String jsonPayload) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(Constants.resource("remote_snapshot"));
        public static final StreamCodec<FriendlyByteBuf, Snapshot> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.configId(), ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH);
                    buffer.writeVarLong(payload.revision());
                    buffer.writeUtf(payload.jsonPayload(), ConfigSyncAuthority.MAX_JSON_LENGTH);
                },
                buffer -> new Snapshot(
                        buffer.readUtf(ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH),
                        buffer.readVarLong(),
                        buffer.readUtf(ConfigSyncAuthority.MAX_JSON_LENGTH)
                )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record EditRequest(long requestId, String configId, long baseRevision, String draftJson)
            implements CustomPacketPayload {
        public static final Type<EditRequest> TYPE = new Type<>(Constants.resource("remote_edit"));
        public static final StreamCodec<FriendlyByteBuf, EditRequest> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarLong(payload.requestId());
                    buffer.writeUtf(payload.configId(), ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH);
                    buffer.writeVarLong(payload.baseRevision());
                    buffer.writeUtf(payload.draftJson(), ConfigSyncAuthority.MAX_JSON_LENGTH);
                },
                buffer -> new EditRequest(
                        buffer.readVarLong(),
                        buffer.readUtf(ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH),
                        buffer.readVarLong(),
                        buffer.readUtf(ConfigSyncAuthority.MAX_JSON_LENGTH)
                )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record EditResult(
            long requestId,
            String configId,
            ConfigEditStatus status,
            long revision,
            String snapshotJson,
            String detail
    ) implements CustomPacketPayload {
        public static final Type<EditResult> TYPE = new Type<>(Constants.resource("remote_result"));
        public static final StreamCodec<FriendlyByteBuf, EditResult> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarLong(payload.requestId());
                    buffer.writeUtf(payload.configId(), ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH);
                    buffer.writeUtf(payload.status().name(), 32);
                    buffer.writeVarLong(payload.revision());
                    buffer.writeUtf(payload.snapshotJson(), ConfigSyncAuthority.MAX_JSON_LENGTH);
                    buffer.writeUtf(payload.detail(), ConfigSyncAuthority.MAX_DETAIL_LENGTH);
                },
                buffer -> new EditResult(
                        buffer.readVarLong(),
                        buffer.readUtf(ConfigSyncAuthority.MAX_CONFIG_ID_LENGTH),
                        decodeStatus(buffer.readUtf(32)),
                        buffer.readVarLong(),
                        buffer.readUtf(ConfigSyncAuthority.MAX_JSON_LENGTH),
                        buffer.readUtf(ConfigSyncAuthority.MAX_DETAIL_LENGTH)
                )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static ConfigEditStatus decodeStatus(String value) {
        try {
            return ConfigEditStatus.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return ConfigEditStatus.INVALID;
        }
    }
}
//?}
