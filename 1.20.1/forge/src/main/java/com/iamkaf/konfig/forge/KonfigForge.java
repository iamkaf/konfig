package com.iamkaf.konfig.forge;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigCommon;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.sync.v1.KonfigSync;
import com.iamkaf.konfig.sync.v1.KonfigSyncPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

@Mod(Constants.MOD_ID)
public final class KonfigForge {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Constants.resource("main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public KonfigForge() {
        RuntimeEnvironment.initialize(FMLPaths.CONFIGDIR.get(), FMLLoader.getDist().isClient());
        KonfigCommon.init();

        if (FMLLoader.getDist().isClient()) {
            KonfigForgeClient.init();
        }

        CHANNEL.registerMessage(0, SyncMessage.class, SyncMessage::encode, SyncMessage::decode,
                (message, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    context.enqueueWork(() -> {
                        if (context.getSender() == null) {
                            KonfigSync.onClientSnapshot(message.configId, message.jsonPayload);
                        }
                    });
                    context.setPacketHandled(true);
                });

        KonfigSync.setSender((player, snapshot) ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMessage(snapshot.configId(), snapshot.jsonPayload()))
        );

        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeave);
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KonfigSync.onPlayerJoin(player);
        }
    }

    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (FMLLoader.getDist().isClient()) {
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
            KonfigSyncPayload.encode(new KonfigSyncPayload(message.configId, message.jsonPayload), buffer);
        }

        private static SyncMessage decode(FriendlyByteBuf buffer) {
            KonfigSyncPayload payload = KonfigSyncPayload.decode(buffer);
            return new SyncMessage(payload.configId(), payload.jsonPayload());
        }
    }
}
