package ru.obvilion.discordrpc;

import ru.obvilion.discordrpc.obj.Packet;
import ru.obvilion.discordrpc.api.User;
import ru.obvilion.json.JSONObject;

public interface DiscordRPCListener {
    default void onPacketSent(DiscordRPC client, Packet packet) {}

    default void onPacketReceived(DiscordRPC client, Packet packet) {}

    default void onActivityJoin(DiscordRPC client, String secret) {}

    default void onActivitySpectate(DiscordRPC client, String secret) {}

    default void onActivityJoinRequest(DiscordRPC client, String secret, User user) {}

    default void onReady(DiscordRPC client) {}

    default void onClose(DiscordRPC client, JSONObject json) {}

    default void onDisconnect(DiscordRPC client, Throwable t) {}
}
