package edu.tfg.raft.client;

import edu.tfg.raft.core.ClusterConfig;
import edu.tfg.raft.statemachine.Command;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cliente que envia comandos a cualquier nodo del cluster; si el nodo
 * contactado no es el lider, sigue la pista {@code leaderHint} para
 * redirigirse al lider real en el siguiente intento.
 */
public final class RaftClient {

    private final Map<String, ClusterConfig.NodeAddress> peers;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String knownLeaderId;

    public RaftClient(Map<String, ClusterConfig.NodeAddress> peers) {
        this.peers = peers;
    }

    /** Devuelve "OK" para SET/DELETE, o el valor (o null) para GET. */
    public String submit(Command.Type type, String key, String value) throws IOException, InterruptedException {
        for (String peerId : candidateOrder()) {
            ClusterConfig.NodeAddress addr = peers.get(peerId);
            JSONObject reqBody = new JSONObject().put("type", type.name()).put("key", key);
            if (value != null) {
                reqBody.put("value", value);
            }
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + addr.host() + ":" + addr.port() + "/client/command"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(reqBody.toString()))
                    .build();

            HttpResponse<String> resp;
            try {
                resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                continue; // nodo inalcanzable, probar el siguiente
            }

            JSONObject respJson = new JSONObject(resp.body());
            if (resp.statusCode() == 200) {
                knownLeaderId = peerId;
                if (type == Command.Type.GET) {
                    return respJson.isNull("value") ? null : respJson.getString("value");
                }
                return "OK";
            }
            if (respJson.has("leaderHint") && !respJson.isNull("leaderHint")) {
                knownLeaderId = respJson.getString("leaderHint");
            }
        }
        throw new IOException("No se pudo contactar con el lider tras probar todos los nodos: " + peers.keySet());
    }

    private List<String> candidateOrder() {
        List<String> ids = new ArrayList<>();
        if (knownLeaderId != null) {
            ids.add(knownLeaderId);
        }
        for (String id : peers.keySet()) {
            if (!id.equals(knownLeaderId)) {
                ids.add(id);
            }
        }
        return ids;
    }
}
