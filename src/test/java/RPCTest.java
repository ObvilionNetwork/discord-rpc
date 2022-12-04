import ru.obvilion.discordrpc.DiscordRPC;
import ru.obvilion.discordrpc.DiscordRPCListener;
import ru.obvilion.discordrpc.api.RichPresence;
import ru.obvilion.discordrpc.api.User;
import ru.obvilion.discordrpc.exceptions.NoDiscordException;
import ru.obvilion.json.JSONObject;

import java.time.OffsetDateTime;

public class RPCTest {
    public static void main(String[] args) throws NoDiscordException {
        DiscordRPC rpc = new DiscordRPC("657878741703327754");

        rpc.setListener(new DiscordRPCListener() {
            public void onActivityJoin(DiscordRPC client, String secret) {
                System.out.println("RPC Join - " + secret);
            }

            public void onActivitySpectate(DiscordRPC client, String secret) {
                System.out.println("RPC Spectate - " + secret);
            }

            public void onActivityJoinRequest(DiscordRPC client, String secret, User user) {
                System.out.println("RPC Join request - " + secret + ", " + user);
            }

            public void onReady(DiscordRPC client) {
                System.out.println("RPC Ready");
            }

            public void onClose(DiscordRPC client, JSONObject json) {
                System.out.println("RPC Closed");
            }

            public void onDisconnect(DiscordRPC client, Throwable t) {
                System.out.println("RPC Disconnected");
            }
        });

        rpc.connect();

        rpc.sendRichPresence(
                new RichPresence.Builder()
                        .setDetails("Hello,")
                        .setState("Discord RPC!")
                        .setStartTimestamp(OffsetDateTime.now())
                        .build()
        );
    }
}
