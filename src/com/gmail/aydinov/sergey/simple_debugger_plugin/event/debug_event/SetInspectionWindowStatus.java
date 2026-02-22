package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;

public class SetInspectionWindowStatus extends AbstractSimpleDebugEvent {
	
	private final boolean show;

	public SetInspectionWindowStatus(boolean show) {
		super(SimpleDebuggerEventType.INSPECTION_WINDOW_SHOW);
		this.show = show;
	}

	public boolean shouldBeShown() {
		return show;
	}
}
