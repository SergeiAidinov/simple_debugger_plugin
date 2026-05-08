package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.DataProviderHolderImpl;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DataProviderHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

public class UserCancelledLoading implements UIEventHandler {

	@Override
	public boolean handle(HandlerContext context, AbstractUIEvent event) {
		System.out.println("CANCELLED <==============");
		UIEvent<Long> uiEvent = (UIEvent<Long>) event;
		InspectionHandlerContext inspectionHandlerContext = (InspectionHandlerContext) context;
		DataProviderHolder q = inspectionHandlerContext.getInspectionSeanceCache().getDataProviderHolders().get(uiEvent.getPayload());
		q.terminateCurrentRequest();
		return false;
	}

}
