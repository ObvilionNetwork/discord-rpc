package ru.obvilion.discordrpc.pipe.impl;

import ru.obvilion.discordrpc.DiscordRPC;
import ru.obvilion.discordrpc.obj.Callback;
import ru.obvilion.discordrpc.obj.Packet;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import ru.obvilion.discordrpc.pipe.Pipe;
import ru.obvilion.discordrpc.pipe.PipeStatus;
import ru.obvilion.json.JSONException;
import ru.obvilion.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class UnixPipe extends Pipe {
    private final AFUNIXSocket socket;

    public UnixPipe(DiscordRPC discordRPC, HashMap<String, Callback> callbacks, String location) throws IOException {
        super(discordRPC, callbacks);

        socket = AFUNIXSocket.newInstance();
        socket.connect(AFUNIXSocketAddress.of(new File(location)));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public Packet read() throws IOException, JSONException {
        InputStream is = socket.getInputStream();

        while(is.available() == 0 && status == PipeStatus.CONNECTED)
        {
            try {
                Thread.sleep(50);
            } catch(InterruptedException ignored) {}
        }

        if (status == PipeStatus.DISCONNECTED) {
            throw new IOException("Disconnected!");
        }

        if (status == PipeStatus.CLOSED) {
            return new Packet(Packet.OpCode.CLOSE, null);
        }

        // Read the op and length. Both are signed ints
        byte[] d = new byte[8];
        is.read(d);
        ByteBuffer bb = ByteBuffer.wrap(d);

        Packet.OpCode op = Packet.OpCode.values()[Integer.reverseBytes(bb.getInt())];
        d = new byte[Integer.reverseBytes(bb.getInt())];

        is.read(d);
        Packet p = new Packet(op, new JSONObject(new String(d)));

        if (listener != null) {
            listener.onPacketReceived(discordRPC, p);
        }

        return p;
    }

    @Override
    public void write(byte[] b) throws IOException {
        socket.getOutputStream().write(b);
    }

    @Override
    public void close() throws IOException {
        send(Packet.OpCode.CLOSE, new JSONObject(), null);

        status = PipeStatus.CLOSED;
        socket.close();
    }
}
