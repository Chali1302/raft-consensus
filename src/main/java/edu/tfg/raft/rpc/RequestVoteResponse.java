package edu.tfg.raft.rpc;

public record RequestVoteResponse(long term, boolean voteGranted) {
}
