package edu.tfg.raft.core;

public final class SystemRaftClock implements RaftClock {
    @Override
    public long nowMillis() {
        return System.currentTimeMillis();
    }
}
