package edu.tfg.raft.core;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Escribe eventos internos de Raft como lineas JSON (events.jsonl), una por
 * evento. De aqui se derivan luego las metricas de tiempo de eleccion /
 * recuperacion tras fallo.
 */
public final class EventLogger implements Closeable {

    private final String nodeId;
    private final BufferedWriter writer;

    public EventLogger(String nodeId, Path filePath) throws IOException {
        this.nodeId = nodeId;
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        this.writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public synchronized void log(long term, String role, String event, Map<String, Object> extra) {
        JSONObject obj = new JSONObject();
        obj.put("ts", System.currentTimeMillis());
        obj.put("nodeId", nodeId);
        obj.put("term", term);
        obj.put("role", role);
        obj.put("event", event);
        if (extra != null && !extra.isEmpty()) {
            obj.put("extra", extra);
        }
        try {
            writer.write(obj.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        writer.close();
    }
}
