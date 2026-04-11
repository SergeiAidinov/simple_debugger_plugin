package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

import java.util.EnumSet;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers.IgnoreEverntHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers.UserChangedFieldHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers.UserChangedVariableHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers.UserClosedDebugWindowHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers.UserInvokedMethodHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers.UserObjectInspectionHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers.UserPressedResumeButtonHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers.UserRequestedAdditionalInfoAboutCollectionHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers.UserRerquestedAdditionalInfoAboutObjectHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TopLevelElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserInvokedMethodEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;

/**
 * Types of events emitted by the simple debugger.
 *
 * Author: Sergei Aidinov Email: sergey.aydinov@gmail.com
 */
public final class SimpleDebuggerEventTypes {

	private SimpleDebuggerEventTypes() {
		// Utility class, prevent instantiation
	}

	/**
	 * Enum of all event types.
	 */
	public enum SimpleDebuggerEventType {

		// ============= DEBUG EVENTS =============
		/** Event triggered when the debugger stops at a breakpoint */
		STOPPED_AT_BREAKPOINT(DebugWindowDataDTO.class, null),

		/** Event triggered to refresh the debugger console */
		REFRESH_CONSOLE(String.class, null),

		SET_RESUME_BUTTON_STATE(Boolean.class, new IgnoreEverntHandler()),

		/** Event triggered when a method is invoked in the target application */
		METHOD_INVOKE(String.class, null),

		DISPLAY_ADDITIONAL_INFO(UserInstanceDetailsDTO.class, null),

		DISPLAY_INSPECTION_WINDOW(Boolean.class, null),

		// ============= USER INTERFACE DEBUG WINDOW EVENTS =============

		USER_PRESSED_RESUME_BUTTON(Void.class, new UserPressedResumeButtonHandler()),

		USER_CHANGED_FIELD(UserChangedFieldEventDTO.class, new UserChangedFieldHandler()),

		USER_CHANGED_VARIABLE(UserChangedVariableEventDTO.class, new UserChangedVariableHandler()),

		USER_CLOSED_DEBUG_WINDOW(Void.class, new UserClosedDebugWindowHandler()),

		USER_INVOKED_METHOD(UserInvokedMethodEventDTO.class, new UserInvokedMethodHandler()),

		// ============= USER INTERFACE INSPECTION WINDOW EVENTS =============

		SHOW_ANCHOR_ELEMENT(TopLevelElementRepresentationDTO.class, null),

		USER_REQUESTED_ADDITIONAL_INFO_ABOUT_OBJECT(InnerElementRepresentationDTO.class,
				new UserRerquestedAdditionalInfoAboutObjectHandler()),
//
		USER_REQUESTED_ADDITIONAL_INFO_ABOUT_COLLECTION(InnerElementRepresentationDTO.class,
				new UserRequestedAdditionalInfoAboutCollectionHandler()),

		//USER_STARTED_INSPECTION_SESSION_FOR_ELEMENT(InnerElementRepresentationDTO.class, null),

		//USER_ENDED_INSPECTION_SESSION_FOR_ELEMENT(Void.class, null),

		USER_STARTED_INSPECTION_SEANCE(InnerElementRepresentationDTO.class,
				null),
		
		USER_CLOSED_INSPECTION_SEANCE(null, new IgnoreEverntHandler()),

		SET_COLLECTION_INSPECT_WINDOW_STATE(Boolean.class, null), 
		
		USER_REQUESTED_COLLECTION_PAGE(Integer.class, null),
		
		DISPLAY_PAGE_OF_INSPECTABLE_ITERABLE(ArrayPageDTO.class, null), 
		
		
		// === MAP ===

//		USER_STARTED_INSPECTION_SEANCE_FOR_MAP(InnerElementRepresentationDTO.class,
//		        new MapInspectionSeanceHandler()),
//
//		USER_CLOSED_INSPECTION_SEANCE_FOR_MAP(Void.class, new IgnoreEverntHandler()),

		USER_REQUESTED_MAP_PAGE(Integer.class, null),

		DISPLAY_PAGE_OF_INSPECTABLE_MAP(MapPageDTO.class, null), 
	//	USER_STARTED_INSPECTION_SEANCE_FOR_USER_OBJECT(UserInstanceInspectionDTO.class, new UserObjectInspectionHandler()), 
		DISPLAY_PAGE_OF_INSPECTABLE_USER_OBJECT(UserObjectPageDTO.class, null), 
		USER_CONTINUES_INSPECTION_FOR_USER_OBJECT(InnerElementRepresentationDTO.class, new UserObjectInspectionHandler()),

		
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

		public UIEventHandler getUiEventHandler() {
			return uiEventHandler;
		}

	}

	// --- Groups of event types ---
	
	private static final EnumSet<SimpleDebuggerEventType> INSPECTION_EVENTS = EnumSet.of(
			SimpleDebuggerEventType.SET_COLLECTION_INSPECT_WINDOW_STATE,
			SimpleDebuggerEventType.USER_STARTED_INSPECTION_SEANCE,
			SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE,
			SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE,
			SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_ITERABLE,
			SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_MAP,
			SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_USER_OBJECT

	);

//	private static final EnumSet<SimpleDebuggerEventType> INSPECTION_WINDOW_EVENTS = EnumSet
//			.of(SimpleDebuggerEventType.SHOW_ANCHOR_ELEMENT);
	
//	private static final EnumSet<SimpleDebuggerEventType> INSPECTIONAL_EVENTS =
//	        EnumSet.copyOf(INSPECTION_EVENTS);

//	static {
//	    INSPECTIONAL_EVENTS.add(SimpleDebuggerEventType.USER_STARTED_INSPECTION_SEANCE_FOR_USER_OBJECT);
//	}

	/**
	 * Checks if the event is an inspection window event.
	 */
//	public static boolean isInspectionWindowEvent(SimpleDebuggerEventType type) {
//		return INSPECTION_WINDOW_EVENTS.contains(type);
//	}

	/**
	 * Checks if the event is a debug window event.
	 */
//	public static boolean isMainWindowEvent(SimpleDebuggerEventType type) {
//		return !isInspectionWindowEvent(type) && !is;
//	}

//	public static boolean isCollectionInspectionWindowEvent(SimpleDebuggerEventType type) {
//		return INSPECTION_EVENTS.contains(type);
//	}
	
	public static boolean isInspectionEvent(SimpleDebuggerEventType type) {
		return INSPECTION_EVENTS.contains(type);
	}
}