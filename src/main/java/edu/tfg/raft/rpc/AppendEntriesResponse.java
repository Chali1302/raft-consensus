package edu.tfg.raft.rpc;

public record AppendEntriesResponse(long term, boolean success, long matchIndex) {
}
