package edu.tfg.raft.rpc;

import edu.tfg.raft.core.LogEntry;

import java.util.List;

public record AppendEntriesRequest(long term, String leaderId, long prevLogIndex, long prevLogTerm,
                                    List<LogEntry> entries, long leaderCommit) {
}
