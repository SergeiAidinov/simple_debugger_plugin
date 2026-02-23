package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractSimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.EventType;

public class ShowAnchorElement extends AbstractSimpleDebugEvent {
	
	private final TargetApplicationElementRepresentation anchorElement;

	public ShowAnchorElement(TargetApplicationElementRepresentation anchorElement) {
		super(EventType.SHOW_ANCHOR_ELEMENT);
		this.anchorElement = anchorElement;
	}

	public TargetApplicationElementRepresentation getAnchorElement() {
		return anchorElement;
	}
}
