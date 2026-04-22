package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class IterableInspectionSeanceHandler implements UIEventHandler{

	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		// TODO Auto-generated method stub
		return false;
	}

}
