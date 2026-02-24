package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;

public final class DebugEvent<T> extends AbstractDebugEvent {

	private final T payload;

	public DebugEvent(SimpleDebuggerEventTypes.SimpleDebuggerEventType type, T payload) {
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
