package edu.tfg.raft.statemachine;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class CommandCodec {

    private CommandCodec() {
    }

    public static byte[] encode(Command command) {
        JSONObject o = new JSONObject()
                .put("type", command.type().name())
                .put("key", command.key());
        if (command.value() != null) {
            o.put("value", command.value());
        }
        return o.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static Command decode(byte[] bytes) {
        JSONObject o = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        return new Command(
                Command.Type.valueOf(o.getString("type")),
                o.getString("key"),
                o.has("value") ? o.getString("value") : null);
    }
}
