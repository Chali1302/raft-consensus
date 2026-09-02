package edu.tfg.raft.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class ScheduledExecutorRaftScheduler implements RaftScheduler {

    private final ScheduledExecutorService executor;

    public ScheduledExecutorRaftScheduler(String nodeId) {
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "raft-scheduler-" + nodeId);
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public Cancellable scheduleOnce(Runnable task, long delayMs) {
        ScheduledFuture<?> future = executor.schedule(wrap(task), delayMs, TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    @Override
    public Cancellable scheduleAtFixedRate(Runnable task, long initialDelayMs, long periodMs) {
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(wrap(task), initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }

    private static Runnable wrap(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        };
    }
}
