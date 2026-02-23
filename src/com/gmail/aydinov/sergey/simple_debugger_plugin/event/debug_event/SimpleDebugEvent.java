package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractSimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;

public final class SimpleDebugEvent<T> extends AbstractSimpleDebugEvent {

	private final T payload;

	public SimpleDebugEvent(SimpleDebuggerEventTypes.EventType type, T payload) {
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
