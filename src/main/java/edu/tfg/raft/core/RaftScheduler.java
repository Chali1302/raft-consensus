package edu.tfg.raft.core;

public interface RaftScheduler {

    Cancellable scheduleOnce(Runnable task, long delayMs);

    Cancellable scheduleAtFixedRate(Runnable task, long initialDelayMs, long periodMs);

    void shutdown();

    @FunctionalInterface
    interface Cancellable {
        void cancel();
    }
}
