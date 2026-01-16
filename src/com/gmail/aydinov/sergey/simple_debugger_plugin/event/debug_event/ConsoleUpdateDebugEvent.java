package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;

public class ConsoleUpdateDebugEvent extends AbstractSimpleDebugEvent{
	
	 private final String text;

	public ConsoleUpdateDebugEvent(SimpleDebuggerEventType type, String text) {
		super(type);
		this.text = text;
	}
	
	public String getText() {
        return text;
    }

}
