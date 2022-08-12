package ru.obvilion.discordrpc;

import ru.obvilion.discordrpc.ipc.IpcClient;

import java.io.IOException;
import java.net.Socket;

public class RpcClient {
    public static void main(String[] args) throws IOException {
        IpcClient client = new IpcClient("discord-ipc-0");
        client.

        Socket socket = newClientSocket("\\\\.\\pipe\\discord-ipc-0");
        System.err.println(socket.isConnected());
    }
}
