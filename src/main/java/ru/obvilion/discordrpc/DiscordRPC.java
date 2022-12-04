package ru.obvilion.discordrpc;

import ru.obvilion.discordrpc.api.*;
import ru.obvilion.discordrpc.obj.Callback;
import ru.obvilion.discordrpc.obj.Packet;
import ru.obvilion.discordrpc.obj.Packet.OpCode;
import ru.obvilion.discordrpc.pipe.Pipe;
import ru.obvilion.discordrpc.pipe.PipeStatus;
import ru.obvilion.discordrpc.exceptions.NoDiscordException;
import ru.obvilion.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;

public final class DiscordRPC implements Closeable {
    private final String clientId;
    private final HashMap<String, Callback> callbacks = new HashMap<>();
    private volatile Pipe pipe;
    private DiscordRPCListener listener = null;
    private Thread readThread = null;

    public DiscordRPC(long clientId) {
        this.clientId = clientId + "";
    }

    public DiscordRPC(String clientId) {
        this.clientId = clientId;
    }

    public void setListener(DiscordRPCListener listener) {
        this.listener = listener;

        if (pipe != null) {
            pipe.setListener(listener);
        }
    }

    public void connect(DiscordBuild... preferredOrder) throws NoDiscordException {
        checkConnected(false);
        callbacks.clear();
        pipe = null;

        pipe = Pipe.openPipe(this, clientId, callbacks, preferredOrder);

        if (listener != null) {
            listener.onReady(this);
        }

        startReading();
    }

    public void sendRichPresence(RichPresence presence) {
        sendRichPresence(presence, null);
    }

    public void sendRichPresence(RichPresence presence, Callback callback) {
        checkConnected(true);

        pipe.send(OpCode.FRAME, new JSONObject()
                .put("cmd", "SET_ACTIVITY")
                .put("args", new JSONObject()
                        .put("pid", getPID())
                        .put("activity", presence == null ? null : presence.toJson())), callback);
    }

    public void subscribe(Event sub) {
        subscribe(sub, null);
    }

    public void subscribe(Event sub, Callback callback) {
        checkConnected(true);
        if (!sub.isSubscribable()) {
            throw new IllegalStateException("Cannot subscribe to " + sub + " event!");
        }

        pipe.send(OpCode.FRAME, new JSONObject()
                .put("cmd", "SUBSCRIBE")
                .put("evt", sub.name()), callback);
    }

    public PipeStatus getStatus() {
        if (pipe == null) {
            return PipeStatus.UNINITIALIZED;
        }

        return pipe.getStatus();
    }

    @Override
    public void close() {
        checkConnected(true);

        try {
            pipe.close();
        } catch (IOException e) {

        }
    }

    public DiscordBuild getDiscordBuild() {
        if (pipe == null) {
            return null;
        }

        return pipe.getDiscordBuild();
    }

    public enum Event {
        NULL(false), // used for confirmation
        READY(false),
        ERROR(false),

        ACTIVITY_JOIN(true),
        ACTIVITY_SPECTATE(true),
        ACTIVITY_JOIN_REQUEST(true),

        UNKNOWN(false);

        private final boolean subscribable;

        Event(boolean subscribable) {
            this.subscribable = subscribable;
        }

        public boolean isSubscribable() {
            return subscribable;
        }

        static Event of(String str) {
            if (str == null) {
                return NULL;
            }

            for (Event s : Event.values()) {
                if (s != UNKNOWN && s.name().equalsIgnoreCase(str)) {
                    return s;
                }
            }

            System.out.println(str);
            return UNKNOWN;
        }
    }


    private void checkConnected(boolean connected) {
        if (connected && getStatus() != PipeStatus.CONNECTED)
            throw new IllegalStateException(String.format("IPCClient (ID: %d) is not connected!", clientId));

        if (!connected && getStatus() == PipeStatus.CONNECTED)
            throw new IllegalStateException(String.format("IPCClient (ID: %d) is already connected!", clientId));
    }

    private void startReading() {
        readThread = new Thread(() -> {
            try {
                Packet p;
                while ((p = pipe.read()).getOp() != OpCode.CLOSE) {
                    JSONObject json = p.getJson();
                    Event event = Event.of(json.optString("evt", null));
                    String nonce = json.optString("nonce", null);

                    // Получаем ответ на последнюю команду от RPC
                    switch (event) {
                        // ОК
                        case NULL:
                            if (nonce != null && callbacks.containsKey(nonce))
                                callbacks.remove(nonce).succeed(p);
                            break;

                        // ОШИБКА
                        case ERROR:
                            if (nonce != null && callbacks.containsKey(nonce))
                                callbacks.remove(nonce).fail(json.getJSONObject("data").optString("message", null));
                            break;
                    }

                    // DISPATCH указывает на то, что дискорд отправил один из ивентов
                    if (listener != null && json.has("cmd") && json.getString("cmd").equals("DISPATCH")) {
                        try {
                            JSONObject data = json.getJSONObject("data");

                            switch (event) {
                                case ACTIVITY_JOIN:
                                    listener.onActivityJoin(this, data.getString("secret"));
                                    break;

                                case ACTIVITY_SPECTATE:
                                    listener.onActivitySpectate(this, data.getString("secret"));
                                    break;

                                case ACTIVITY_JOIN_REQUEST:
                                    JSONObject u = data.getJSONObject("user");
                                    User user = new User(
                                            u.getString("username"),
                                            u.getString("discriminator"),
                                            u.getString("id"),
                                            u.optString("avatar", null)
                                    );

                                    listener.onActivityJoinRequest(this, data.optString("secret", null), user);
                                    break;
                            }
                        } catch (Exception ignored) {

                        }
                    }
                }

                pipe.setStatus(PipeStatus.DISCONNECTED);

                if (listener != null) {
                    listener.onClose(this, p.getJson());
                }
            } catch (IOException ex) {
                pipe.setStatus(PipeStatus.DISCONNECTED);

                if (listener != null) {
                    listener.onDisconnect(this, ex);
                }
            }
        });

        readThread.start();
    }

    private static int getPID() {
        String pr = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(pr.substring(0, pr.indexOf('@')));
    }
}
