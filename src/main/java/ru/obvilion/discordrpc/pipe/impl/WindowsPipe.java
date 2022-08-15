package ru.obvilion.discordrpc.pipe.impl;

import ru.obvilion.discordrpc.DiscordRPC;
import ru.obvilion.discordrpc.obj.Callback;
import ru.obvilion.discordrpc.obj.Packet;
import ru.obvilion.discordrpc.pipe.Pipe;
import ru.obvilion.discordrpc.pipe.PipeStatus;
import ru.obvilion.json.JSONException;
import ru.obvilion.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

public class WindowsPipe extends Pipe {
    private final RandomAccessFile file;

    public WindowsPipe(DiscordRPC discordRPC, HashMap<String, Callback> callbacks, String location) {
        super(discordRPC, callbacks);
        try {
            this.file = new RandomAccessFile(location, "rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        file.write(b);
    }

    @Override
    public Packet read() throws IOException, JSONException {
        while(file.length() == 0 && status == PipeStatus.CONNECTED)
        {
            try {
                Thread.sleep(50);
            } catch(InterruptedException ignored) {}
        }

        if(status==PipeStatus.DISCONNECTED)
            throw new IOException("Disconnected!");

        if(status==PipeStatus.CLOSED)
            return new Packet(Packet.OpCode.CLOSE, null);

        Packet.OpCode op = Packet.OpCode.values()[Integer.reverseBytes(file.readInt())];
        int len = Integer.reverseBytes(file.readInt());
        byte[] d = new byte[len];

        file.readFully(d);
        Packet p = new Packet(op, new JSONObject(new String(d)));

        if(listener != null)
            listener.onPacketReceived(discordRPC, p);
        return p;
    }

    @Override
    public void close() throws IOException {
        send(Packet.OpCode.CLOSE, new JSONObject(), null);
        status = PipeStatus.CLOSED;
        file.close();
    }

}
