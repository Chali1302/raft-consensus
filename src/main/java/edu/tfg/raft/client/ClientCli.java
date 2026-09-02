package edu.tfg.raft.client;

import edu.tfg.raft.core.ClusterConfig;
import edu.tfg.raft.statemachine.Command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ClientCli {

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parseArgs(args);
        Path configFile = Path.of(require(opts, "config"));
        RaftClient client = new RaftClient(ClusterConfig.loadPeersFromFile(configFile));

        System.out.println("Cliente Raft. Comandos: set <k> <v> | get <k> | del <k> | salir");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+", 3);
            if (parts.length == 0 || parts[0].isEmpty()) {
                continue;
            }
            try {
                switch (parts[0]) {
                    case "set" -> {
                        if (parts.length < 3) {
                            System.out.println("uso: set <clave> <valor>");
                            continue;
                        }
                        client.submit(Command.Type.SET, parts[1], parts[2]);
                        System.out.println("OK");
                    }
                    case "get" -> {
                        if (parts.length < 2) {
                            System.out.println("uso: get <clave>");
                            continue;
                        }
                        String value = client.submit(Command.Type.GET, parts[1], null);
                        System.out.println(value == null ? "(null)" : value);
                    }
                    case "del" -> {
                        if (parts.length < 2) {
                            System.out.println("uso: del <clave>");
                            continue;
                        }
                        client.submit(Command.Type.DELETE, parts[1], null);
                        System.out.println("OK");
                    }
                    case "salir", "exit" -> {
                        return;
                    }
                    default -> System.out.println("comando desconocido: " + parts[0]);
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
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
