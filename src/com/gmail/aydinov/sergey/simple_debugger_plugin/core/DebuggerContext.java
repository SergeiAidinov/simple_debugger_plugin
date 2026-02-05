package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;

/**
 * Singleton context holding the current state of the debugger.
 * Provides thread-safe access and status management.
 */
public class DebuggerContext {

    /**
     * Represents the possible states of the debugger.
     */
    public enum SimpleDebuggerStatus {
        WILL_NOT_START,
        STARTING,
        VM_AWAITING_CONNECTION,
        VM_CONNECTED,
        PREPARING,
        PREPARED,
        RUNNING,
        SESSION_STARTED,
        SESSION_FINISHED,
        STOPPED
    }

    private static final DebuggerContext INSTANCE = new DebuggerContext();
    private final ReentrantLock lock = new ReentrantLock(true); // fair lock
    private volatile SimpleDebuggerStatus status;
    private static final Set<SimpleDebuggerStatus> RUNNING_STATES = EnumSet.of(
            SimpleDebuggerStatus.RUNNING,
            SimpleDebuggerStatus.SESSION_STARTED,
            SimpleDebuggerStatus.SESSION_FINISHED
    );
    private static final Set<SimpleDebuggerStatus> TERMINAL_STATES = EnumSet.of(
            SimpleDebuggerStatus.WILL_NOT_START,
            SimpleDebuggerStatus.STOPPED
    );

    private DebuggerContext() {
        status = SimpleDebuggerStatus.STARTING;
    }

    /**
     * Returns the singleton instance of DebuggerContext.
     *
     * @return the DebuggerContext instance
     */
    public static DebuggerContext context() {
        return INSTANCE;
    }

    // -----------------------
    // Status API

    /**
     * Returns the current status of the debugger.
     *
     * @return the current SimpleDebuggerStatus
     */
    public SimpleDebuggerStatus getStatus() {
        lock.lock();
        try {
            return status;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Updates the debugger status in a thread-safe manner.
     * Logs state transitions if the status actually changes.
     *
     * @param newStatus the new status to set
     */
    public boolean setStatus(SimpleDebuggerStatus newStatus) {
        lock.lock();
        try {
        	if (TERMINAL_STATES.contains(status)) return false;
            if (this.status != newStatus) {
                SimpleDebuggerLogger.info(
                        "Debugger state changed: " + this.status + " -> " + newStatus
                );
                this.status = newStatus;
            }
        } finally {
            lock.unlock();
        }
		return true;
    }

    // -----------------------
    // Derived state

    /**
     * Returns true if the debugger is in a running state.
     *
     * @return true if debugger is running or session is active/finished
     */
    public boolean isRunning() {
        lock.lock();
        try {
            return RUNNING_STATES.contains(status);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Returns whether the debugger is in a non-terminal state.
     * <p>
     * This method returns {@code true} if the debugger has not yet reached
     * an unchangeable (terminal) state such as {@link SimpleDebuggerStatus#STOPPED}
     * or {@link SimpleDebuggerStatus#WILL_NOT_START}.
     * </p>
     *
     * @return {@code true} if the debugger can still change its state,
     *         {@code false} if it is already stopped or cannot be started
     */
    public boolean isInTerminalState() {
        lock.lock();
        try {
            return TERMINAL_STATES.contains(status);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns true if a debugger session is currently active.
     *
     * @return true if the debugger session has started
     */
    public boolean isSessionActive() {
        lock.lock();
        try {
            return status.equals(SimpleDebuggerStatus.SESSION_STARTED);
        } finally {
            lock.unlock();
        }
    }
}
