package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class IgnoreEverntHandler implements UIEventHandler{

	@Override
	public boolean handle(HandlerContext uiEventContext, AbstractUIEvent abstractSimpleDebuggerUIEvent) {
		SimpleDebuggerLogger.info("Intentionally ignored event: " + abstractSimpleDebuggerUIEvent);
		return false;
	}

}
