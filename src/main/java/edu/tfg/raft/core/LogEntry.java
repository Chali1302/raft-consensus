package edu.tfg.raft.core;

public record LogEntry(long term, long index, byte[] command) {
}
