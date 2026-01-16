package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

public class ConsoleUpdateDebugEvent extends AbstractSimpleDebugEventDTO{
	
	 private final String text;

	public ConsoleUpdateDebugEvent(SimpleDebugEventType type, String text) {
		super(type);
		this.text = text;
	}
	
	public String getText() {
        return text;
    }

}
