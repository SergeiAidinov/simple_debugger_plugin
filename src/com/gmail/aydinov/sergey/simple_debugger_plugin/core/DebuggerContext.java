package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.concurrent.atomic.AtomicBoolean;

public class DebuggerContext {

    public enum SimpleDebuggerStatus {
        STARTING,
        VM_AWAITING_CONNECTION,
        VM_CONNECTED
    }

    public enum TargetApplicationStatus {
        STARTING,
        RUNNING,
        STOPPED_AT_BREAKPOINT,
        STOPPING
    }

	private static final DebuggerContext INSTANCE = new DebuggerContext();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile SimpleDebuggerStatus simpleDebuggerStatus;
    private volatile TargetApplicationStatus targetApplicationStatus;

    private DebuggerContext() {
        simpleDebuggerStatus = SimpleDebuggerStatus.STARTING;
        targetApplicationStatus = TargetApplicationStatus.STARTING;
    }

    public static DebuggerContext context() {
        return INSTANCE;
    }

    // -----------------------
    // Running flag
    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean value) {
        running.set(value);
    }

    // -----------------------
    // SimpleDebuggerStatus
    public SimpleDebuggerStatus getSimpleDebuggerStatus() {
        return simpleDebuggerStatus;
    }

    public void setSimpleDebuggerStatus(SimpleDebuggerStatus status) {
        this.simpleDebuggerStatus = status;
    }

    // -----------------------
    // TargetApplicationStatus
    public TargetApplicationStatus getTargetApplicationStatus() {
        return targetApplicationStatus;
    }

    public void setTargetApplicationStatus(TargetApplicationStatus status) {
        this.targetApplicationStatus = status;
    }
}
