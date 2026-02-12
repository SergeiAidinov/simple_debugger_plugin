package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.sun.jdi.event.BreakpointEvent;

/**
 * Functional interface for handling JDI breakpoint events.
 */
@FunctionalInterface
public interface BreakpointEventHandler {

    /**
     * Handles a breakpoint event triggered in the target VM.
     *
     * @param breakpointEvent the breakpoint event to handle
     */
    void handle(BreakpointEvent breakpointEvent);
}
