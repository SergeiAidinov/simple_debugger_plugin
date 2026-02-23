package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

import java.util.Set;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugStoppedAtBreakpointEvent;

/**
 * Types of events emitted by the simple debugger.
 *
 * Author: Sergei Aidinov
 * Email: sergey.aydinov@gmail.com
 */
public final class SimpleDebuggerEventTypes {

    private SimpleDebuggerEventTypes() {
        // Utility class, prevent instantiation
    }

    /**
     * Enum of all event types.
     */
    public enum EventType {

        /** Event triggered when the debugger stops at a breakpoint */
        STOPPED_AT_BREAKPOINT(DebugStoppedAtBreakpointEvent.class),

        /** Event triggered to refresh the debugger console */
        REFRESH_CONSOLE(Void.class),

        SET_RESUME_BUTTON_STATE(Boolean.class),

        /** Event triggered when a method is invoked in the target application */
        METHOD_INVOKE(String.class),

        INSPECTION_WINDOW_SHOW(Void.class),

        SHOW_ANCHOR_ELEMENT(Object.class);

        private final Class<?> payloadType;

        EventType(Class<?> payloadType) {
            this.payloadType = payloadType;
        }

        public Class<?> getPayloadType() {
            return payloadType;
        }
    }

    // --- Groups of event types ---

    private static final Set<EventType> INSPECTION_WINDOW_EVENTS =
            Set.of(EventType.INSPECTION_WINDOW_SHOW);

    /**
     * Checks if the event is an inspection window event.
     */
    public static boolean isInspectionWindowEvent(EventType type) {
        return INSPECTION_WINDOW_EVENTS.contains(type);
    }

    /**
     * Checks if the event is a debug window event.
     */
    public static boolean isDebugWindowEvent(EventType type) {
        return !isInspectionWindowEvent(type);
    }
}