package edu.tfg.raft.statemachine;

import edu.tfg.raft.core.StateMachine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Todas las operaciones (incluido GET) pasan por el log y se aplican solo
 * una vez comprometidas: decision de diseno deliberada para no implementar
 * read-index/leader-lease, y para que la latencia de lectura sea
 * comparable metodologicamente con QBFT/IBFT (que tambien ordenan todo via
 * bloques, lecturas incluidas).
 */
public final class KeyValueStateMachine implements StateMachine {

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    public byte[] apply(byte[] commandBytes) {
        Command command = CommandCodec.decode(commandBytes);
        switch (command.type()) {
            case SET -> store.put(command.key(), command.value());
            case DELETE -> store.remove(command.key());
            case GET -> {
                // no-op: la lectura solo necesita haber pasado por el log/commit
            }
        }
        return new byte[0];
    }

    public String get(String key) {
        return store.get(key);
    }
}
