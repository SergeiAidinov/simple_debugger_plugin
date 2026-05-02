package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeanceCache;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class InspectionSeanceUIEventContext extends DebugHandlerContext {

    private final InspectionSeanceCache inspectionSeanceCache;

    public InspectionSeanceUIEventContext(StackFrame frame,
                                    BreakpointEvent breakpointEvent,
                                    InspectionSeanceCache inspectionSeanceCache) {
        super(frame, breakpointEvent);
        this.inspectionSeanceCache = inspectionSeanceCache;
    }

    public InspectionSeanceCache getInspectionSeanceCache() {
        return inspectionSeanceCache;
    }
}
