package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

public class StatusesHolder {
	
	public enum SimpleDebuggerStatus {
		STARTING, VM_AWAITING_CONNECTION, VM_CONNECTED
	}
	
	public enum TargetApplicationStatus {
		STARTING, RUNNING, STOPPED_AT_BREAKPOINT, STOPPING
	}

	private StatusesHolder() {
	}

	public static SimpleDebuggerStatus simpleDebuggerStatus;
	public static TargetApplicationStatus targetApplicationStatus;
}
