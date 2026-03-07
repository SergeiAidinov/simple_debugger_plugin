package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

/** 
 * Base class for all events in the simple debugger.
 */
public abstract class AbstractSimpleDebuggerEvent {
    private final SimpleDebuggerEventTypes.SimpleDebuggerEventType type;

    protected AbstractSimpleDebuggerEvent(SimpleDebuggerEventTypes.SimpleDebuggerEventType type) {
        this.type = type;
    }

    public SimpleDebuggerEventTypes.SimpleDebuggerEventType getType() {
        return type;
    }
}