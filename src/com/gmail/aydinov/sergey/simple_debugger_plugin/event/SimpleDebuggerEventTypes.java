package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

import java.util.Set;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers.UserChangedVariableHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TopLevelElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserInvokedMethodEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceElementInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceInspectionDTO;

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
        STOPPED_AT_BREAKPOINT(DebugWindowDataDTO.class, null),
        
        /** Event triggered to refresh the debugger console */
        REFRESH_CONSOLE(String.class, null),

        SET_RESUME_BUTTON_STATE(Boolean.class, null),

        /** Event triggered when a method is invoked in the target application */
        METHOD_INVOKE(String.class, null),
        
        DISPLAY_ADDITIONAL_INFO(UserInstanceInspectionDTO.class, null),

        DISPLAY_INSPECTION_WINDOW(Boolean.class, null),
        
      //============= USER INTERFACE DEBUG WINDOW EVENTS =============
        
        USER_PRESSED_RESUME_BUTTON(Void.class, null),
        
        USER_CHANGED_FIELD(UserChangedFieldEventDTO.class,  null),
        
        USER_CHANGED_VARIABLE(UserChangedVariableEventDTO.class, new UserChangedVariableHandler()),
        
        USER_CLOSED_DEBUG_WINDOW(Void.class, null),
        
        USER_INVOKED_METHOD(UserInvokedMethodEventDTO.class, null),
        
      //============= USER INTERFACE INSPECTION WINDOW EVENTS =============
        
        SHOW_ANCHOR_ELEMENT(TopLevelElementRepresentationDTO.class, null),
        
        USER_REQUESTED_ADDITIONAL_INFO_ABOUT_OBJECT(InnerElementRepresentationDTO.class, null),
        
        USER_REQUESTED_ADDITIONAL_INFO_ABOUT_COLLECTION(InnerElementRepresentationDTO.class, null),
        
        USER_STARTED_INSPECTION_SESSION_FOR_ELEMENT(InnerElementRepresentationDTO.class, null),
        
        USER_ENDED_INSPECTION_SESSION_FOR_ELEMENT(Void.class, null)
        
        
        ;

        private final Class<?> payloadType;
        private final UIEventHandler uiEventHandler;

        SimpleDebuggerEventType(Class<?> payloadType, UIEventHandler uiEventHandler) {
            this.payloadType = payloadType;
            this.uiEventHandler = uiEventHandler;
        }

        public Class<?> getPayloadType() {
            return payloadType;
        }

		public UIEventHandler<?> getUiEventHandler() {
			return uiEventHandler;
		}
        
    }
    
    // --- Groups of event types ---

    private static final Set<SimpleDebuggerEventType> INSPECTION_WINDOW_EVENTS =
            Set.of(SimpleDebuggerEventType.SHOW_ANCHOR_ELEMENT);

    /**
     * Checks if the event is an inspection window event.
     */
    public static boolean isInspectionWindowEvent(SimpleDebuggerEventType type) {
        return INSPECTION_WINDOW_EVENTS.contains(type);
    }

    /**
     * Checks if the event is a debug window event.
     */
    public static boolean isDebugWindowEvent(SimpleDebuggerEventType type) {
        return !isInspectionWindowEvent(type);
    }
}