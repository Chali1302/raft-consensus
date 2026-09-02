package edu.tfg.raft.core;

@FunctionalInterface
public interface StateMachine {
    byte[] apply(byte[] command);
}
