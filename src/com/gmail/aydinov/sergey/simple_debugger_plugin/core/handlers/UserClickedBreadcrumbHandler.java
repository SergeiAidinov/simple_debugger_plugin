package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.Map.Entry;
import java.util.Objects;
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

	@SuppressWarnings("unchecked")
	@Override
	public boolean handle(HandlerContext context, AbstractUIEvent event) {
		UIEvent<PairDTO<Integer, ?>> uiEvent = (UIEvent<PairDTO<Integer, ?>>) event;
		InspectionHandlerContext inspectionHandlerContext = (InspectionHandlerContext) context;
		BreadCrumb breadCrumb = inspectionHandlerContext.getInspectionSeanceCache().getBreadcrumbs()
				.get(uiEvent.getPayload().getFirst());
		if (Objects.nonNull(breadCrumb)) {
			inspectionHandlerContext.setHeaderOrder(uiEvent.getPayload().getFirst());
			uiEventCollector.submitPriorityEvent(breadCrumb.getAbstractUIEvent());
		}
		return false;
	}

}
