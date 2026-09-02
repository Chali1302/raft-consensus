package edu.tfg.raft.rpc;

import edu.tfg.raft.core.LogEntry;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * (De)serializacion manual, mensaje a mensaje, para que la forma exacta de
 * cada RPC de Raft quede visible en el codigo (sin reflection generica).
 */
public final class JsonCodec {

    private JsonCodec() {
    }

    public static String toJson(RequestVoteRequest r) {
        return new JSONObject()
                .put("term", r.term())
                .put("candidateId", r.candidateId())
                .put("lastLogIndex", r.lastLogIndex())
                .put("lastLogTerm", r.lastLogTerm())
                .toString();
    }

    public static RequestVoteRequest requestVoteRequestFromJson(String json) {
        JSONObject o = new JSONObject(json);
        return new RequestVoteRequest(o.getLong("term"), o.getString("candidateId"),
                o.getLong("lastLogIndex"), o.getLong("lastLogTerm"));
    }

    public static String toJson(RequestVoteResponse r) {
        return new JSONObject()
                .put("term", r.term())
                .put("voteGranted", r.voteGranted())
                .toString();
    }

    public static RequestVoteResponse requestVoteResponseFromJson(String json) {
        JSONObject o = new JSONObject(json);
        return new RequestVoteResponse(o.getLong("term"), o.getBoolean("voteGranted"));
    }

    public static String toJson(AppendEntriesRequest r) {
        JSONArray entries = new JSONArray();
        for (LogEntry e : r.entries()) {
            entries.put(new JSONObject()
                    .put("term", e.term())
                    .put("index", e.index())
                    .put("command", Base64.getEncoder().encodeToString(e.command())));
        }
        return new JSONObject()
                .put("term", r.term())
                .put("leaderId", r.leaderId())
                .put("prevLogIndex", r.prevLogIndex())
                .put("prevLogTerm", r.prevLogTerm())
                .put("entries", entries)
                .put("leaderCommit", r.leaderCommit())
                .toString();
    }

    public static AppendEntriesRequest appendEntriesRequestFromJson(String json) {
        JSONObject o = new JSONObject(json);
        JSONArray arr = o.getJSONArray("entries");
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject eo = arr.getJSONObject(i);
            entries.add(new LogEntry(eo.getLong("term"), eo.getLong("index"),
                    Base64.getDecoder().decode(eo.getString("command"))));
        }
        return new AppendEntriesRequest(o.getLong("term"), o.getString("leaderId"),
                o.getLong("prevLogIndex"), o.getLong("prevLogTerm"), entries, o.getLong("leaderCommit"));
    }

    public static String toJson(AppendEntriesResponse r) {
        return new JSONObject()
                .put("term", r.term())
                .put("success", r.success())
                .put("matchIndex", r.matchIndex())
                .toString();
    }

    public static AppendEntriesResponse appendEntriesResponseFromJson(String json) {
        JSONObject o = new JSONObject(json);
        return new AppendEntriesResponse(o.getLong("term"), o.getBoolean("success"), o.getLong("matchIndex"));
    }
}
