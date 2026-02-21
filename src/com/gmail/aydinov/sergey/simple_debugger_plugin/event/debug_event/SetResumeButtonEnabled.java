package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;

public class SetResumeButtonEnabled extends AbstractSimpleDebugEvent {
	
	private final boolean enable;

	public SetResumeButtonEnabled(boolean enable) {
		super(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE);
		this.enable = enable;
	}

	public boolean shouldBeEnabled() {
		return enable;
	}
}
