package com.iamkaf.konfig.forge;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigCommon;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.sync.v1.KonfigSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

@Mod(Constants.MOD_ID)
public final class KonfigForge {
    private static final int PROTOCOL = 1;
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Constants.resource("main"))
            .networkProtocolVersion(PROTOCOL)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .simpleChannel();

    public KonfigForge() {
        RuntimeEnvironment.initialize(FMLPaths.CONFIGDIR.get(), FMLLoader.getDist().isClient());
        KonfigCommon.init();

        if (FMLLoader.getDist().isClient()) {
            KonfigForgeClient.init();
        }

        CHANNEL.messageBuilder(SyncMessage.class)
                .encoder(SyncMessage::encode)
                .decoder(SyncMessage::decode)
                .consumerMainThread((message, context) -> {
                    if (context.getSender() == null) {
                        KonfigSync.onClientSnapshot(message.configId, message.jsonPayload);
                    }
                })
                .add();

        KonfigSync.setSender((player, snapshot) ->
                CHANNEL.send(new SyncMessage(snapshot.configId(), snapshot.jsonPayload()), PacketDistributor.PLAYER.with(player))
        );

        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(this::onPlayerJoin);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(this::onPlayerLeave);
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KonfigSync.onPlayerJoin(player);
        }
    }

    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide()) {
            KonfigSync.onClientDisconnect();
        }
    }

    private static final class SyncMessage {
        private final String configId;
        private final String jsonPayload;

        private SyncMessage(String configId, String jsonPayload) {
            this.configId = configId;
            this.jsonPayload = jsonPayload;
        }

        private static void encode(SyncMessage message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.configId, 256);
            buffer.writeUtf(message.jsonPayload);
        }

        private static SyncMessage decode(FriendlyByteBuf buffer) {
            return new SyncMessage(buffer.readUtf(256), buffer.readUtf());
        }
    }
}
