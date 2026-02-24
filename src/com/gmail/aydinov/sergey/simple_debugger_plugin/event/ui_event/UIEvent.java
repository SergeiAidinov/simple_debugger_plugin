package com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event;

import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;

public class UIEvent<T> extends AbstractSimpleDebuggerUIEvent{
	
	private final T payload;

	public UIEvent(SimpleDebuggerEventType type, T payload) {
		super(type);
		if (Objects.nonNull(payload) && !type.getPayloadType().isInstance(payload)) {
			throw new IllegalArgumentException("Payload type mismatch. Expected: " + type.getPayloadType().getName()
					+ ", got: " + payload.getClass().getName());
		}
		this.payload = payload;
	}
	
	public T getPayload() {
		return payload;
	}
}
