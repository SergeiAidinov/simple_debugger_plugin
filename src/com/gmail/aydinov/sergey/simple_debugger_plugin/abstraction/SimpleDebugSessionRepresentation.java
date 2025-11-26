package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import com.sun.jdi.event.BreakpointEvent;

public class SimpleDebugSessionRepresentation {
	
	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
	private final BreakpointEvent breakpointEvent;
	
	public SimpleDebugSessionRepresentation(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
			BreakpointEvent breakpointEvent) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
		this.breakpointEvent = breakpointEvent;
	}

	public TargetVirtualMachineRepresentation getTargetVirtualMachineRepresentation() {
		return targetVirtualMachineRepresentation;
	}

	public BreakpointEvent getBreakpointEvent() {
		return breakpointEvent;
	}
	
}
