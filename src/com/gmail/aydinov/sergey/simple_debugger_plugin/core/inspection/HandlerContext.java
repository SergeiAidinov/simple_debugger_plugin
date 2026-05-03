package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public abstract class HandlerContext {

    private final StackFrame frame;
    private final BreakpointEvent breakpointEvent;

    protected HandlerContext(StackFrame frame, BreakpointEvent breakpointEvent) {
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