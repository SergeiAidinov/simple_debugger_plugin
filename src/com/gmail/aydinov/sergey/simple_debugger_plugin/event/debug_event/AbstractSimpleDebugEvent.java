package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;

public class AbstractSimpleDebugEvent {
	
	private final SimpleDebuggerEventType type;

	public AbstractSimpleDebugEvent(SimpleDebuggerEventType type) {
		this.type = type;
	}

	public SimpleDebuggerEventType getType() {
		return type;
	}
	
}
