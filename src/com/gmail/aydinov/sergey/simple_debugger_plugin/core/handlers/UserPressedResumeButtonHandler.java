package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class UserPressedResumeButtonHandler implements UIEventHandler{

	@Override
	public boolean handle(HandlerContext abstractUIEventContext, AbstractUIEvent abstractSimpleDebuggerUIEvent) {
		SimpleDebuggerLogger.info("User pressed RESUME");
		DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_FINISHED);
		
		return true;
	}

}
