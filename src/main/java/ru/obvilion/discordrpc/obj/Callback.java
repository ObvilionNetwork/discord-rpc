package ru.obvilion.discordrpc.obj;

import java.util.function.Consumer;

public class Callback {
    private final Consumer<Packet> success;
    private final Consumer<String> failure;

    public Callback() {
        this((Consumer<Packet>) null, null);
    }

    public Callback(Consumer<Packet> success) {
        this(success, null);
    }

    public Callback(Consumer<Packet> success, Consumer<String> failure) {
        this.success = success;
        this.failure = failure;
    }

    public Callback(Runnable success, Consumer<String> failure) {
        this(p -> success.run(), failure);
    }

    public Callback(Runnable success) {
        this(p -> success.run(), null);
    }

    public boolean isEmpty() {
        return success == null && failure == null;
    }

    public void succeed(Packet packet) {
        if (success != null) {
            success.accept(packet);
        }
    }

    public void fail(String message) {
        if (failure != null) {
            failure.accept(message);
        }
    }
}
