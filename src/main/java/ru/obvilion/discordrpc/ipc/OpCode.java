package ru.obvilion.discordrpc.ipc;

import java.util.Arrays;
import java.util.Optional;

public enum OpCode {
    HANDSHAKE (0),
    FRAME     (1),
    CLOSE     (2),
    PING      (3),
    PONG      (4),
    OTHER     (-1);


    final int id;
    OpCode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static OpCode byId(int id) {
       Optional<OpCode> code = Arrays.stream(OpCode.values())
               .filter(opCode -> opCode.id == id)
               .findFirst();

       return code.orElse(OTHER);
    }
}
