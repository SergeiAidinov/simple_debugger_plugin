package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.DebugSessionUIEventContext;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class InspectionSeanceContex extends DebugSessionUIEventContext{
	
	private final InspectionSeanceCache inspectionSeanceCache;
	  public InspectionSeanceContex(StackFrame frame, BreakpointEvent breakpointEvent, InspectionSeanceCache inspectionSeanceCache) {
		super(frame, breakpointEvent);
		this.inspectionSeanceCache = inspectionSeanceCache;
	}
	  public InspectionSeanceCache getInspectionSeanceCache() {
		  return inspectionSeanceCache;
	  }
}
