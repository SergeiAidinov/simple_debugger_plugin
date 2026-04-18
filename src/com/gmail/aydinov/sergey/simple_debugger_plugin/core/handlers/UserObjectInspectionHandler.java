package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class UserObjectInspectionHandler implements UIEventHandler {

	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();

	@SuppressWarnings("unchecked")
	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		System.out.println("USER OBJECT. INSP. STARTED");
		TargetApplicationRepresentation.getInstance().getAllElements().stream().forEach(e -> System.out.println(e));
		debugEventCollector
				.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, false));
		UIEvent<InnerElementRepresentationDTO> uiEvent = null;
		try {
			uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
		} catch (ClassCastException castException) {

		}
		if (Objects.nonNull(uiEvent)) {
			InnerElementRepresentationDTO userObject = uiEvent.getPayload();
			List<UniversalElementRepresentation> uObjs = findAllUserObjects(userObject);
			List<InnerElementRepresentationDTO> fields = new ArrayList<InnerElementRepresentationDTO>();
			List<InnerElementRepresentationDTO> methods = new ArrayList<InnerElementRepresentationDTO>();
			List<UniversalElementRepresentation> classElementList = new ArrayList<UniversalElementRepresentation>();
			for (UniversalElementRepresentation universalElementRepresentation : uObjs) {
				fields.addAll(TargetApplicationRepresentation.getInstance().getAllElements().stream()
						.filter(e -> e instanceof UniversalElementRepresentation)
						.map(e -> (UniversalElementRepresentation) e)
						.filter(e -> Objects.equals(e.getTag().getParentId(),
								universalElementRepresentation.getTag().getUniqueId()))
						.toList().stream()
						.map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e))
						.toList());
				classElementList.addAll(TargetApplicationRepresentation.getInstance().getAllElements().stream()
						.filter(e -> e instanceof UniversalElementRepresentation)
						.map(e -> (UniversalElementRepresentation) e)
						.filter(e -> Objects.equals(e.getAdditionalInfo(),
								universalElementRepresentation.getAdditionalInfo()))
						.filter(e -> Objects.isNull(e.getTag().getParentId())).toList());
			}
			for (UniversalElementRepresentation classRepresentation : classElementList) {
				methods.addAll(TargetApplicationRepresentation.getInstance().getAllElements().stream()
						.filter(e -> e instanceof UniversalElementRepresentation)
						.map(e -> (UniversalElementRepresentation) e)
						.filter(e -> e.getElementType() == UniversalElementType.METHOD)
						.filter(e -> Objects.equals(e.getTag().getParentId(),
								classRepresentation.getTag().getUniqueId()))
						.toList().stream()
						.map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e))
						.toList());
			}
			List<InnerElementRepresentationDTO> subordinates = new ArrayList<InnerElementRepresentationDTO>(fields);
			subordinates.addAll(methods);
			subordinates = subordinates.stream().distinct().toList();
			UserObjectPageDTO userObjectPageDTO = UserObjectPageDTO.builder().elementName(userObject.getElementName())
					.entries(subordinates).classType(userObject.getTypeOrReturnType()).anchorTag(userObject.getTag())
					.build();
			debugEventCollector.collectDebugEvent(new DebugEvent<>(
					SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_USER_OBJECT, userObjectPageDTO));
		}
		return true;

	}

	private List<UniversalElementRepresentation> findAllUserObjects(InnerElementRepresentationDTO anchor) {
		return TargetApplicationRepresentation.getInstance().getAllElements().stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.nonNull(e.getObjectReference())).filter(e -> Objects
						.equals(String.valueOf(e.getObjectReference().uniqueID()), anchor.getAdditionalInfo()))
				.toList();
	}
}
