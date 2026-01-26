package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;

public class MethodInvokedEvent extends AbstractSimpleDebugEvent {
	
	private final String resultOfInvocation;

	public MethodInvokedEvent(SimpleDebuggerEventType type, String resultOfinvocation) {
		super(type);
		this.resultOfInvocation = resultOfinvocation;
	}

	public String getResultOfInvocation() {
		return resultOfInvocation;
	}
	
}
