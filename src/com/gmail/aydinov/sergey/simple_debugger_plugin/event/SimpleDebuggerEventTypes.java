package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

import java.util.Set;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugStoppedAtBreakpointDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.FieldOrVariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.dto.UserChangedFieldEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.dto.UserChangedVariableEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.dto.UserInvokedMethodEventDTO;

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
    public enum SimpleDebuggerEventType {

    	//============= DEBUG EVENTS =============
        /** Event triggered when the debugger stops at a breakpoint */
        STOPPED_AT_BREAKPOINT(DebugStoppedAtBreakpointDTO.class),

        /** Event triggered to refresh the debugger console */
        REFRESH_CONSOLE(String.class),

        SET_RESUME_BUTTON_STATE(Boolean.class),

        /** Event triggered when a method is invoked in the target application */
        METHOD_INVOKE(String.class),

        DISPLAY_INSPECTION_WINDOW(Boolean.class),
        
      //============= USER INTERFACE DEBUG WINDOW EVENTS =============
        
        USER_PRESSED_RESUME_BUTTON(Void.class),
        
        USER_CHANGED_FIELD(UserChangedFieldEventDTO.class),
        
        USER_CHANGED_VARIABLE(UserChangedVariableEventDTO.class),
        
        USER_CLOSED_DEBUG_WINDOW(Void.class),
        
        USER_INVOKED_METHOD(UserInvokedMethodEventDTO.class),





        
        
      //============= USER INTERFACE INSPECTION WINDOW EVENTS =============
        
        SHOW_ANCHOR_ELEMENT(TargetApplicationElementRepresentation.class),
        
        USER_STARTED_INSPECTION_SESSION_FOR_ELEMENT(FieldOrVariableDTO.class),
        
        USER_ENDED_INSPECTION_SESSION_FOR_ELEMENT(Void.class)


        ;

        private final Class<?> payloadType;

        SimpleDebuggerEventType(Class<?> payloadType) {
            this.payloadType = payloadType;
        }

        public Class<?> getPayloadType() {
            return payloadType;
        }
    }
    
    // --- Groups of event types ---

    private static final Set<SimpleDebuggerEventType> INSPECTION_WINDOW_EVENTS =
            Set.of(SimpleDebuggerEventType.SHOW_ANCHOR_ELEMENT);

    /**
     * Checks if the event is an inspection window event.
     */
    public static boolean addressEventToInspectionWindow(SimpleDebuggerEventType type) {
        return INSPECTION_WINDOW_EVENTS.contains(type);
    }

    /**
     * Checks if the event is a debug window event.
     */
    public static boolean handleEventInContextOfDebugWindow(SimpleDebuggerEventType type) {
        return !addressEventToInspectionWindow(type);
    }
}