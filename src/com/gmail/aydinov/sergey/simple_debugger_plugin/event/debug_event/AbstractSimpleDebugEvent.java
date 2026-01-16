package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventType;

public class AbstractSimpleDebugEvent {
	
	private final SimpleDebugEventType type;

	public AbstractSimpleDebugEvent(SimpleDebugEventType type) {
		this.type = type;
	}

	public SimpleDebugEventType getType() {
		return type;
	}
	
}
