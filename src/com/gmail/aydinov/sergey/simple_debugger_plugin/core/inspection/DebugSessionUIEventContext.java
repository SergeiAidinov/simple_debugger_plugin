package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class DebugSessionUIEventContext extends AbstractUIEventContext {
    private final StackFrame frame;
    private final BreakpointEvent breakpointEvent;

    public DebugSessionUIEventContext(StackFrame frame, BreakpointEvent breakpointEvent) {
        this.frame = frame;
        this.breakpointEvent = breakpointEvent;
    }

    public StackFrame getFrame() {
        return frame;
    }

    public BreakpointEvent getBreakpointEvent() {
        return breakpointEvent;
    }
}
