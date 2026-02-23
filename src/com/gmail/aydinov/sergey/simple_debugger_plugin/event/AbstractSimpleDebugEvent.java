package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

/**
 * Base class for all events emitted by the simple debugger.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class AbstractSimpleDebugEvent {

    /** Type of this debugger event */
    private final SimpleDebuggerEventTypes.EventType type;

    /**
     * Creates a new debugger event with the specified type.
     *
     * @param type the type of the debugger event
     */
    public AbstractSimpleDebugEvent(SimpleDebuggerEventTypes.EventType type) {
        this.type = type;
    }

    /**
     * Returns the type of this debugger event.
     *
     * @return the event type
     */
    public SimpleDebuggerEventTypes.EventType getType() {
        return type;
    }
}
