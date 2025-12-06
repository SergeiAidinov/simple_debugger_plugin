package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

public class AbstractSimpleDebugEventDTO {
	
	private final SimpleDebugEventType type;

	public AbstractSimpleDebugEventDTO(SimpleDebugEventType type) {
		this.type = type;
	}

	public SimpleDebugEventType getType() {
		return type;
	}
	
}
