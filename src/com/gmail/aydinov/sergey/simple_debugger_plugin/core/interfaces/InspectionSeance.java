package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public interface InspectionSeance {
	
	void startInspectionSeanceForAnchorElement(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame, BreakpointEvent breakpointEvent);

}
