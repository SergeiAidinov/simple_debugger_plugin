package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;

/**
 * Singleton context holding the current state of the debugger.
 * Provides thread-safe access and status management.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class DebuggerContext {

    /**
     * Represents the possible states of the debugger.
     */
    public enum SimpleDebuggerStatus {
        DEBUGGER_WILL_NOT_START,
        DEBUGGER_STARTING,
        VM_AWAITING_CONNECTION,
        VM_CONNECTED,
        DEBUGGER_STARTED,
        DEBUG_SESSION_PREPARING,
        DEBUG_SESSION_PREPARED,
        DEBUG_SESSION_RUNNING,
        //COLLECTION_INSPECTION_SEANCE_STARTING,
        INSPECTION_SEANCE_RUNNING,
       // INSPECTION_SEANCE_CLOSING,
       // INSPECTION_SEANCE_STOPPED,
        DEBUG_SESSION_FINISHED,
        DEBUGGER_STOPPED,
    }

    private static final DebuggerContext INSTANCE = new DebuggerContext();
    private final ReentrantLock lock = new ReentrantLock(true); // fair lock
    private volatile SimpleDebuggerStatus status;
    private final AtomicInteger inspectionSeanceId = new AtomicInteger(0);
    private static final Set<SimpleDebuggerStatus> DEBUGGER_RUNNING_STATES = EnumSet.of(
    		SimpleDebuggerStatus.DEBUGGER_STARTED,
    		SimpleDebuggerStatus.DEBUG_SESSION_RUNNING,
    		//SimpleDebuggerStatus.COLLECTION_INSPECTION_SEANCE_STARTING,
    		SimpleDebuggerStatus.INSPECTION_SEANCE_RUNNING,
    		//SimpleDebuggerStatus.INSPECTION_SEANCE_CLOSING,
    		//SimpleDebuggerStatus.INSPECTION_SEANCE_STOPPED,
    		SimpleDebuggerStatus.DEBUG_SESSION_FINISHED
    );
    private static final Set<SimpleDebuggerStatus> TERMINAL_STATES = EnumSet.of(
            SimpleDebuggerStatus.DEBUGGER_WILL_NOT_START,
            SimpleDebuggerStatus.DEBUGGER_STOPPED
    );
    
    private static final Set<SimpleDebuggerStatus> INSPECTION_SEANCE_RUNNING_STATES = EnumSet.of(
    		//SimpleDebuggerStatus.COLLECTION_INSPECTION_SEANCE_STARTING,
    		SimpleDebuggerStatus.INSPECTION_SEANCE_RUNNING
    		//SimpleDebuggerStatus.INSPECTION_SEANCE_CLOSING
    );

    private DebuggerContext() {
        status = SimpleDebuggerStatus.DEBUGGER_STARTING;
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
            return DEBUGGER_RUNNING_STATES.contains(status);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Checks whether the debugger has reached a terminal state.
     * <p>
     * A terminal state is a status from which the debugger cannot transition
     * to a running or startable state. This includes
     * {@link SimpleDebuggerStatus#DEBUGGER_STOPPED} and
     * {@link SimpleDebuggerStatus#DEBUGGER_WILL_NOT_START}.
     * </p>
     *
     * @return {@code true} if the debugger is in a terminal state and cannot be started or resumed,
     *         {@code false} if the debugger can still transition to another state.
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
    public boolean isDebugSessionActive() {
        lock.lock();
        try {
            return status.equals(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
        } finally {
            lock.unlock();
        }
    }
    
    public boolean isInspectionSeanceActive() {
        lock.lock();
        try {
            return INSPECTION_SEANCE_RUNNING_STATES.contains(status);
        } finally {
            lock.unlock();
        }
    }
    
    public int getInspectionSeanceId() {
    	return inspectionSeanceId.get();
    }
    
    public int defineInspectionSeanceId() {
    	return inspectionSeanceId.incrementAndGet();
    }
}
