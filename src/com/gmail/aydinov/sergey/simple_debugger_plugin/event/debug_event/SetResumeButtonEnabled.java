package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;

public class SetResumeButtonEnabled extends AbstractSimpleDebugEvent {
	
	private final boolean enable;

	public SetResumeButtonEnabled(boolean enable) {
		super(SimpleDebuggerEventTypes.EventType.SET_RESUME_BUTTON_STATE);
		this.enable = enable;
	}

	public boolean shouldBeEnabled() {
		return enable;
	}
}
