package ru.obvilion.discordrpc.ipc;

import org.scalasbt.ipcsocket.UnixDomainSocket;
import org.scalasbt.ipcsocket.Win32NamedPipeSocket;
import ru.obvilion.discordrpc.util.OsUtil;

import java.io.IOException;
import java.net.Socket;

public class IpcClient {
    private final String pipe_name;
    private Socket socket;

    public IpcClient(String pipe_name) {
        this.pipe_name = pipe_name;
    }

    public void connect() throws IOException {
        String ipc_path = getIpcPath();

        this.socket = OsUtil.isWindows()
                ? new Win32NamedPipeSocket(ipc_path, false)
                : new UnixDomainSocket(ipc_path, false);
    }

    public Socket getSocket() {
        return this.socket;
    }

//    public static encode(OpCode op, data: object) {
//        const str = JSON.stringify(data);
//        const len = Buffer.byteLength(str);
//        const packet = Buffer.alloc(8 + len);
//        packet.writeInt32LE(op, 0);
//        packet.writeInt32LE(len, 4);
//        packet.write(str, 8, len);
//        return packet;
//    }

    public String getIpcPath() {
        switch (OsUtil.CURRENT_OS) {
            case WINDOWS:
                return "\\\\?\\pipe\\" + this.pipe_name;
            case LINUX:
                return System.getenv("XDG_RUNTIME_DIR") + "/" + this.pipe_name;
            case MAC:
                return System.getenv("TMPDIR") + "/" + this.pipe_name;
            default:
                throw new RuntimeException(
                       "Platform '" + System.getProperty("os.name") + "' is not supported at the moment"
                );
        }
    }
}
