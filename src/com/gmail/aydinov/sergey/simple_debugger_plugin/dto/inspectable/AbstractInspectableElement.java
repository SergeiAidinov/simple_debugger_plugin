package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.inspectable;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public abstract class AbstractInspectableElement {
	
	private final Tag tag;
	
	public AbstractInspectableElement(Tag tag) {
		this.tag = tag;
	}

	UIEventHandler uiEventHandler;
	
	public Tag getTag() {
		return tag;
	}

	public static Factory factory() {
		return new Factory();
	}

	public static class Factory {

		private Factory() {
		}

		public AbstractInspectableElement createInspectableElement(AbstractUIEvent event, StackFrame currentFrame,
				BreakpointEvent breakpointEvent) {
			return switch (event.getType()) {
			case USER_STARTED_INSPECTION_SEANCE -> {
				if (!(event instanceof UIEvent<?> rawEvent)) {
					throw new IllegalArgumentException("Invalid event type: " + event);
				}
				Object payload = rawEvent.getPayload();
				if (!(payload instanceof InnerElementRepresentationDTO dto)) {
					throw new IllegalArgumentException("Invalid payload: " + payload);
				}
				yield new InspectableCollection(dto, currentFrame, breakpointEvent);
			}
			default -> throw new UnsupportedOperationException("Unsupported event: " + event.getType());
			};
		}

	}
}
