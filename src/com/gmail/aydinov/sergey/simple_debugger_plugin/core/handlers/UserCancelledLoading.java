package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.Optional;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.PageableDataProviderHolderImpl;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.holder.DataProviderHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

public class UserCancelledLoading implements UIEventHandler {

	@Override
	public boolean handle(HandlerContext context, AbstractUIEvent event) {
		@SuppressWarnings("unchecked")
		UIEvent<Long> uiEvent = (UIEvent<Long>) event;
		InspectionHandlerContext inspectionHandlerContext = (InspectionHandlerContext) context;
		Optional.ofNullable(
				inspectionHandlerContext.getInspectionSeanceCache().getDataProviderHolders().get(uiEvent.getPayload()))
				.ifPresent(dataProvider -> dataProvider.terminateCurrentRequest());
		return false;
	}

}
