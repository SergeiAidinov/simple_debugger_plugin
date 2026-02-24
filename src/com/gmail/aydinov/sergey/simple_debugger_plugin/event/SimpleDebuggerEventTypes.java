package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

import java.util.Set;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugStoppedAtBreakpointDTO;

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
    public enum DebugEventType {

        /** Event triggered when the debugger stops at a breakpoint */
        STOPPED_AT_BREAKPOINT(DebugStoppedAtBreakpointDTO.class),

        /** Event triggered to refresh the debugger console */
        REFRESH_CONSOLE(String.class),

        SET_RESUME_BUTTON_STATE(Boolean.class),

        /** Event triggered when a method is invoked in the target application */
        METHOD_INVOKE(String.class),

        DISPLAY_INSPECTION_WINDOW(Boolean.class),

        SHOW_ANCHOR_ELEMENT(TargetApplicationElementRepresentation.class);

        private final Class<?> payloadType;

        DebugEventType(Class<?> payloadType) {
            this.payloadType = payloadType;
        }

        public Class<?> getPayloadType() {
            return payloadType;
        }
    }

    // --- Groups of event types ---

    private static final Set<DebugEventType> INSPECTION_WINDOW_EVENTS =
            Set.of(DebugEventType.DISPLAY_INSPECTION_WINDOW, DebugEventType.SHOW_ANCHOR_ELEMENT);

    /**
     * Checks if the event is an inspection window event.
     */
    public static boolean isInspectionWindowEvent(DebugEventType type) {
        return INSPECTION_WINDOW_EVENTS.contains(type);
    }

    /**
     * Checks if the event is a debug window event.
     */
    public static boolean isDebugWindowEvent(DebugEventType type) {
        return !isInspectionWindowEvent(type);
    }
}