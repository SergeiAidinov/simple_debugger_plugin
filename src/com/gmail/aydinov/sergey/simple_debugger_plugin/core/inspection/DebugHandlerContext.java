package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class DebugHandlerContext extends HandlerContext {

    public DebugHandlerContext(StackFrame frame, BreakpointEvent breakpointEvent) {
        super(frame, breakpointEvent);
    }
}