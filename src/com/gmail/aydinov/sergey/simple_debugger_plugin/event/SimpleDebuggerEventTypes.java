package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

import java.util.Set;

public class SimpleDebuggerEventTypes {

	/**
	 * Types of events emitted by the simple debugger.
	 * <p>
	 * Author: Sergei Aidinov
	 * <br>
	 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
	 * </p>
	 */
	public enum EventType {

	    /** Event triggered when the debugger stops at a breakpoint */
	    STOPPED_AT_BREAKPOINT,

	    /** Event triggered to refresh the debugger console */
	    REFRESH_CONSOLE,
	    
	    SET_RESUME_BUTTON_STATE,

	    /** Event triggered when a method is invoked in the target application */
	    METHOD_INVOKE,
	    
	    INSPECTION_WINDOW_SHOW

	}
	
	private static final Set<EventType> inspectionWindowEvents = Set.of(EventType.INSPECTION_WINDOW_SHOW);
	
	public static boolean isDebugWindowEvent(EventType eventType) {
		return !inspectionWindowEvents.contains(eventType);
	}
}
