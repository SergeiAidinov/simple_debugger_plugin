package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class UserRequestedCollectionPageHandler implements UIEventHandler{

	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>> uiEvent = null;
		try {
			uiEvent = (UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>>) abstractSimpleDebuggerUIEvent;
		} catch (ClassCastException castException) {

		}
		final long id = uiEvent.getPayload().getFirst().getObjectId();
		Map<Long, UniversalElementRepresentation> map = TargetApplicationRepresentation.getInstance().getAllElements()
				.stream().filter(UniversalElementRepresentation.class::isInstance)
				.map(UniversalElementRepresentation.class::cast).filter(e -> e.getObjectReference() != null)
				.collect(Collectors.toMap(e -> (Long) e.getObjectReference().uniqueID(), Function.identity(),
						(existing, duplicate) -> existing));

		UniversalElementRepresentation mapRepresentation = map.get(id);	
		System.out.println(mapRepresentation);
		
		
		
		
		return false;
	}

}
