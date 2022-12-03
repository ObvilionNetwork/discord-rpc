package ru.obvilion.discordrpc.pipe;

import ru.obvilion.discordrpc.DiscordRPC;
import ru.obvilion.discordrpc.DiscordRPCListener;
import ru.obvilion.discordrpc.obj.Callback;
import ru.obvilion.discordrpc.api.DiscordBuild;
import ru.obvilion.discordrpc.obj.Packet;
import ru.obvilion.discordrpc.exceptions.NoDiscordException;
import ru.obvilion.discordrpc.pipe.impl.UnixPipe;
import ru.obvilion.discordrpc.pipe.impl.WindowsPipe;
import ru.obvilion.json.JSONException;
import ru.obvilion.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public abstract class Pipe {
    private static final int VERSION = 1;
    public PipeStatus status = PipeStatus.CONNECTING;
    public DiscordRPCListener listener;
    private DiscordBuild build;
    public final DiscordRPC discordRPC;
    private final HashMap<String,Callback> callbacks;

    public Pipe(DiscordRPC discordRPC, HashMap<String, Callback> callbacks) {
        this.discordRPC = discordRPC;
        this.callbacks = callbacks;
    }

    public static Pipe openPipe(DiscordRPC discordRPC, String clientId, HashMap<String,Callback> callbacks,
                                DiscordBuild... preferredOrder) throws NoDiscordException {

        if (preferredOrder == null || preferredOrder.length == 0)
            preferredOrder = new DiscordBuild[]{ DiscordBuild.ANY };

        Pipe pipe = null;

        // store some files so we can get the preferred client
        Pipe[] open = new Pipe[DiscordBuild.values().length];
        for (int i = 0; i < 10; i++) {
            try {
                String location = getPipeLocation(i);
                pipe = createPipe(discordRPC, callbacks, location);

                pipe.send(Packet.OpCode.HANDSHAKE, new JSONObject().put("v", VERSION).put("client_id", clientId), null);

                Packet p = pipe.read(); // this is a valid client at this point

                pipe.build = DiscordBuild.from(p.getJson().getJSONObject("data")
                        .getJSONObject("config")
                        .getString("api_endpoint"));

                // we're done if we found our first choice
                if (pipe.build == preferredOrder[0] || DiscordBuild.ANY == preferredOrder[0]) {
                    break;
                }

                open[pipe.build.ordinal()] = pipe; // didn't find first choice yet, so store what we have
                open[DiscordBuild.ANY.ordinal()] = pipe; // also store in 'any' for use later

                pipe.build = null;
                pipe = null;
            } catch(IOException | JSONException ex) {
                pipe = null;
            }
        }

        if (pipe == null) {
            // we already know we don't have our first pick
            // check each of the rest to see if we have that
            for (int i = 1; i < preferredOrder.length; i++) {
                DiscordBuild cb = preferredOrder[i];

                if (open[cb.ordinal()] != null) {
                    pipe = open[cb.ordinal()];
                    open[cb.ordinal()] = null;

                    // if we pulled this from the 'any' slot, we need to figure out which build it was
                    if (cb == DiscordBuild.ANY) {
                        for(int k = 0; k < open.length; k++)
                        {
                            if(open[k] == pipe)
                            {
                                pipe.build = DiscordBuild.values()[k];
                                open[k] = null; // we don't want to close this
                            }
                        }
                    }
                    else pipe.build = cb;

                    break;
                }
            }

            if (pipe == null) {
                throw new NoDiscordException();
            }
        }

        // close unused files, except skip 'any' because its always a duplicate
        for (int i = 0; i < open.length; i++) {
            if (i == DiscordBuild.ANY.ordinal()) {
                continue;
            }

            if (open[i] != null) {
                try {
                    open[i].close();
                } catch(IOException ex) {
                    // This isn't really important to applications and better
                    // as debug info
                }
            }
        }

        pipe.status = PipeStatus.CONNECTED;

        return pipe;
    }

    private static Pipe createPipe(DiscordRPC discordRPC, HashMap<String, Callback> callbacks, String location) {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return new WindowsPipe(discordRPC, callbacks, location);
        }

        try {
            return new UnixPipe(discordRPC, callbacks, location);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(Packet.OpCode op, JSONObject data, Callback callback) {
        try {
            String nonce = generateNonce();
            Packet p = new Packet(op, data.put("nonce",nonce));

            if (callback != null && !callback.isEmpty()) {
                callbacks.put(nonce, callback);
            }

            write(p.toBytes());

            if (listener != null) {
                listener.onPacketSent(discordRPC, p);
            }
        } catch(IOException ex) {
            status = PipeStatus.DISCONNECTED;
        }
    }

    public abstract Packet read() throws IOException, JSONException;

    public abstract void write(byte[] b) throws IOException;

    private static String generateNonce() {
        return UUID.randomUUID().toString();
    }

    public PipeStatus getStatus() {
        return status;
    }

    public void setStatus(PipeStatus status) {
        this.status = status;
    }

    public void setListener(DiscordRPCListener listener) {
        this.listener = listener;
    }

    public abstract void close() throws IOException;

    public DiscordBuild getDiscordBuild() {
        return build;
    }

    private final static String[] unixPaths = { "XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP" };

    private static String getPipeLocation(int id) {
        if (System.getProperty("os.name").contains("Win")) {
            return "\\\\?\\pipe\\discord-ipc-" + id;
        }

        String tmp_path = null;
        for (String str : unixPaths) {
            tmp_path = System.getenv(str);

            if (tmp_path != null) {
                break;
            }
        }

        if (tmp_path == null) {
            tmp_path = "/tmp";
        }

        return tmp_path + "/discord-ipc-" + id;
    }
}
