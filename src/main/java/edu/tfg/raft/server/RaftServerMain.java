package edu.tfg.raft.server;

import edu.tfg.raft.core.ClusterConfig;
import edu.tfg.raft.core.EventLogger;
import edu.tfg.raft.core.InMemoryPersistentStore;
import edu.tfg.raft.core.PersistentStore;
import edu.tfg.raft.core.RaftLog;
import edu.tfg.raft.core.RaftNode;
import edu.tfg.raft.core.RaftScheduler;
import edu.tfg.raft.core.ScheduledExecutorRaftScheduler;
import edu.tfg.raft.rpc.HttpRaftTransport;
import edu.tfg.raft.rpc.RaftRpcServer;
import edu.tfg.raft.statemachine.KeyValueStateMachine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class RaftServerMain {

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parseArgs(args);
        String id = require(opts, "id");
        Path configFile = Path.of(require(opts, "config"));
        Path dataDir = Path.of(opts.getOrDefault("data-dir", "data/" + id));
        Files.createDirectories(dataDir);

        ClusterConfig config = ClusterConfig.loadFromFile(id, configFile);

        EventLogger eventLogger = new EventLogger(id, dataDir.resolve("events.jsonl"));
        PersistentStore store = new InMemoryPersistentStore(); // Dia 3: FilePersistentStore
        RaftLog log = new RaftLog();
        HttpRaftTransport transport = new HttpRaftTransport(config, Duration.ofMillis(200));
        RaftScheduler scheduler = new ScheduledExecutorRaftScheduler(id);
        KeyValueStateMachine stateMachine = new KeyValueStateMachine();

        RaftNode node = new RaftNode(id, config, log, store, transport, scheduler, eventLogger, stateMachine);

        RaftRpcServer server = new RaftRpcServer(config.selfAddress(), node, stateMachine);
        server.start();
        node.start();

        System.out.println("Nodo " + id + " escuchando en puerto " + config.selfAddress().port());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            node.stop();
            server.stop();
            try {
                eventLogger.close();
            } catch (Exception ignored) {
            }
        }));

        Thread.currentThread().join();
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                opts.put(parts[0], parts.length > 1 ? parts[1] : "true");
            }
        }
        return opts;
    }

    private static String require(Map<String, String> opts, String key) {
        String v = opts.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Falta argumento --" + key);
        }
        return v;
    }
}
