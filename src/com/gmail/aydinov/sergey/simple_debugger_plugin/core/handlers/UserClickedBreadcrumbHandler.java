package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.BreadCrumb;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

public class UserClickedBreadcrumbHandler implements UIEventHandler {
	
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

	@Override
	public boolean handle(HandlerContext context, AbstractUIEvent event) {
		UIEvent<PairDTO> uiEvent = (UIEvent<PairDTO>) event;
		InspectionHandlerContext inspectionHandlerContext = (InspectionHandlerContext) context;
		 Optional<Entry<Integer, BreadCrumb>> entryOpt = inspectionHandlerContext.getInspectionSeanceCache().getBreadcrumbs().entrySet().stream()
		.filter(e -> e.getKey().equals(uiEvent.getPayload().getFirst())).findAny();
		 if (entryOpt.isPresent()) {
			 uiEventCollector.submitPriorityEvent(entryOpt.get().getValue().getAbstractUIEvent());
		 }
		return false;
	}

}
