package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class InspectionSeanceUIEventContext extends DebugSessionUIEventContext {

    private final InspectionSeance inspectionSeance;

    public InspectionSeanceUIEventContext(StackFrame frame,
                                    BreakpointEvent breakpointEvent,
                                    InspectionSeance inspectionSeance) {
        super(frame, breakpointEvent);
        this.inspectionSeance = inspectionSeance;
    }

    public InspectionSeance getInspectionSeance() {
        return inspectionSeance;
    }
}
