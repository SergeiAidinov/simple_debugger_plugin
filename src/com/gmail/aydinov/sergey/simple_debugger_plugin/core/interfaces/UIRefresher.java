package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.sun.jdi.event.BreakpointEvent;

/**
 * Interface responsible for refreshing the user interface when a breakpoint is hit.
 */
@FunctionalInterface
public interface UIRefresher {

    /**
     * Refreshes the UI for the given breakpoint event.
     *
     * @param breakpointEvent the {@link BreakpointEvent} that triggered the UI refresh
     * @return true if the UI was successfully refreshed, false otherwise
     */
    boolean refreshUI(BreakpointEvent breakpointEvent);
}
