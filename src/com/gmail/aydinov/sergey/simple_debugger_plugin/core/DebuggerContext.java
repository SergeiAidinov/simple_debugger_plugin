package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;

public class DebuggerContext {

	public enum SimpleDebuggerStatus {
		NOT_STARTED, STARTING, VM_AWAITING_CONNECTION, VM_CONNECTED, PREPARED, RUNNING, SESSION_STARTED,
		SESSION_FINISHED, STOPPED
	}

	private static final DebuggerContext INSTANCE = new DebuggerContext();

	private final ReentrantLock lock = new ReentrantLock(true); // fair lock
	private volatile SimpleDebuggerStatus status;
	private static final Set<SimpleDebuggerStatus> RUNNING_STATES = EnumSet.of(SimpleDebuggerStatus.RUNNING,
			SimpleDebuggerStatus.SESSION_STARTED, SimpleDebuggerStatus.SESSION_FINISHED

	);

	private DebuggerContext() {
		status = SimpleDebuggerStatus.STARTING;
	}

	public static DebuggerContext context() {
		return INSTANCE;
	}

	// -----------------------
	// Status API

	public SimpleDebuggerStatus getStatus() {
		lock.lock();
		try {
			return status;
		} finally {
			lock.unlock();
		}
	}

	public void setStatus(SimpleDebuggerStatus newStatus) {
	    lock.lock();
	    try {
	        if (this.status != newStatus) {
	            SimpleDebuggerLogger.info(
	                "Debugger state: " + this.status + " -> " + newStatus
	            );
	            this.status = newStatus;
	        }
	    } finally {
	        lock.unlock();
	    }
	}


	// -----------------------
	// Derived state

	public boolean isRunning() {
		lock.lock();
		try {
			return RUNNING_STATES.contains(status);
		} finally {
			lock.unlock();
		}
	}

	public boolean isSessionActive() {
		lock.lock();
		try {
			return status.equals(SimpleDebuggerStatus.SESSION_STARTED);
		} finally {
			lock.unlock();
		}
	}

}
