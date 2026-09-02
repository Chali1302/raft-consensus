package edu.tfg.raft.statemachine;

public record Command(Type type, String key, String value) {
    public enum Type {
        SET,
        GET,
        DELETE
    }
}
