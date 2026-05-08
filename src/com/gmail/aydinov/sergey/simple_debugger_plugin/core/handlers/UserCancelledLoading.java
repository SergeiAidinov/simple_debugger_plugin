package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

public class UserCancelledLoading implements UIEventHandler {

	@Override
	public boolean handle(HandlerContext context, AbstractUIEvent event) {
		System.out.println("CANCELLED <==============");
		return false;
	}

}
