package dev.doorevisuals.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PayloadRelay {
    private static boolean bound;

    private PayloadRelay() {
    }

    public static void initServer() {
        if (!bound) {
            bound = true;
            PayloadTypeRegistry.playC2S().register(DoorePayloads.HandshakePayload.TYPE, DoorePayloads.HandshakePayload.CODEC);
            PayloadTypeRegistry.playC2S().register(DoorePayloads.CosmeticsPayload.TYPE, DoorePayloads.CosmeticsPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(DoorePayloads.HandshakePayload.TYPE, DoorePayloads.HandshakePayload.CODEC);
            PayloadTypeRegistry.playS2C().register(DoorePayloads.CosmeticsPayload.TYPE, DoorePayloads.CosmeticsPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(DoorePayloads.EmotePayload.TYPE, DoorePayloads.EmotePayload.CODEC);
            PayloadTypeRegistry.playS2C().register(DoorePayloads.EmotePayload.TYPE, DoorePayloads.EmotePayload.CODEC);
            ServerPlayNetworking.registerGlobalReceiver(
                DoorePayloads.EmotePayload.TYPE,
                // The sender is taken from the connection, not from the packet, so nobody can play an emote for someone else.
                (payload, ctx) -> ctx.server().execute(() -> relay(ctx.player(), new DoorePayloads.EmotePayload(ctx.player().getUuid(), payload.index())))
            );
            ServerPlayNetworking.registerGlobalReceiver(
                DoorePayloads.HandshakePayload.TYPE, (payload, ctx) -> ctx.server().execute(() -> relay(ctx.player(), payload))
            );
            ServerPlayNetworking.registerGlobalReceiver(
                DoorePayloads.CosmeticsPayload.TYPE, (payload, ctx) -> ctx.server().execute(() -> relay(ctx.player(), payload))
            );
        }
    }

    private static void relay(ServerPlayerEntity sender, DoorePayloads.EmotePayload payload) {
        MinecraftServer minecraftserver = sender.getEntityWorld().getServer();
        if (minecraftserver != null) {
            for (ServerPlayerEntity serverplayerentity : PlayerLookup.tracking(sender)) {
                if (serverplayerentity != sender) {
                    ServerPlayNetworking.send(serverplayerentity, payload);
                }
            }
        }
    }

    private static void relay(ServerPlayerEntity sender, DoorePayloads.HandshakePayload payload) {
        MinecraftServer minecraftserver = sender.getEntityWorld().getServer();
        if (minecraftserver != null) {
            for (ServerPlayerEntity serverplayerentity : PlayerLookup.all(minecraftserver)) {
                if (serverplayerentity != sender) {
                    ServerPlayNetworking.send(serverplayerentity, payload);
                }
            }
        }
    }

    private static void relay(ServerPlayerEntity sender, DoorePayloads.CosmeticsPayload payload) {
        MinecraftServer minecraftserver = sender.getEntityWorld().getServer();
        if (minecraftserver != null) {
            for (ServerPlayerEntity serverplayerentity : PlayerLookup.all(minecraftserver)) {
                if (serverplayerentity != sender) {
                    ServerPlayNetworking.send(serverplayerentity, payload);
                }
            }
        }
    }
}
