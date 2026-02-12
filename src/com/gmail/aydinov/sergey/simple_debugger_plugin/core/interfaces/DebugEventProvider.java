package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugStoppedAtBreakpointEvent;

/**
 * Functional interface for sending debug events related to breakpoints.
 */
@FunctionalInterface
public interface DebugEventProvider {

    /**
     * Sends a debug event indicating that the target application has stopped at a breakpoint.
     *
     * @param stoppedEvent the breakpoint stop event
     */
    void sendDebugEvent(DebugStoppedAtBreakpointEvent stoppedEvent);
}
