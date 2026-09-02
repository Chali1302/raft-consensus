package edu.tfg.raft.rpc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.tfg.raft.core.ClusterConfig;
import edu.tfg.raft.core.RaftNode;
import edu.tfg.raft.statemachine.Command;
import edu.tfg.raft.statemachine.CommandCodec;
import edu.tfg.raft.statemachine.KeyValueStateMachine;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class RaftRpcServer {

    private static final long CLIENT_COMMAND_TIMEOUT_MS = 2000;
    private static final long CLIENT_POLL_INTERVAL_MS = 5;

    private final HttpServer httpServer;

    public RaftRpcServer(ClusterConfig.NodeAddress bindAddress, RaftNode raftNode,
                          KeyValueStateMachine stateMachine) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(bindAddress.port()), 0);
        httpServer.setExecutor(Executors.newFixedThreadPool(4));
        httpServer.createContext("/raft/requestVote", exchange -> {
            RequestVoteRequest req = JsonCodec.requestVoteRequestFromJson(readBody(exchange));
            respond(exchange, 200, JsonCodec.toJson(raftNode.handleRequestVote(req)));
        });
        httpServer.createContext("/raft/appendEntries", exchange -> {
            AppendEntriesRequest req = JsonCodec.appendEntriesRequestFromJson(readBody(exchange));
            respond(exchange, 200, JsonCodec.toJson(raftNode.handleAppendEntries(req)));
        });
        httpServer.createContext("/admin/status", exchange -> respond(exchange, 200, raftNode.statusJson()));
        httpServer.createContext("/client/command", exchange -> {
            JSONObject reqJson = new JSONObject(readBody(exchange));
            Command command = new Command(
                    Command.Type.valueOf(reqJson.getString("type")),
                    reqJson.getString("key"),
                    reqJson.has("value") ? reqJson.getString("value") : null);

            RaftNode.SubmitResult result = raftNode.submitCommand(CommandCodec.encode(command));
            if (!result.leader()) {
                JSONObject resp = new JSONObject().put("ok", false).put("error", "NOT_LEADER");
                if (result.leaderHint() != null) {
                    resp.put("leaderHint", result.leaderHint());
                }
                respond(exchange, 409, resp.toString());
                return;
            }

            long index = result.index();
            long deadline = System.currentTimeMillis() + CLIENT_COMMAND_TIMEOUT_MS;
            while (!raftNode.isApplied(index) && System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(CLIENT_POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (!raftNode.isApplied(index)) {
                respond(exchange, 504, new JSONObject().put("ok", false).put("error", "TIMEOUT").toString());
                return;
            }

            JSONObject resp = new JSONObject().put("ok", true).put("index", index);
            if (command.type() == Command.Type.GET) {
                String value = stateMachine.get(command.key());
                resp.put("value", value == null ? JSONObject.NULL : value);
            }
            respond(exchange, 200, resp.toString());
        });
        httpServer.createContext("/admin/shutdown", exchange -> {
            respond(exchange, 200, "{\"ok\":true}");
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
                System.exit(0);
            }).start();
        });
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
