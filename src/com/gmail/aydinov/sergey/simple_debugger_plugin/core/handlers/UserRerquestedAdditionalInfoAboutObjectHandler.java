package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceInnerElementInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class UserRerquestedAdditionalInfoAboutObjectHandler implements UIEventHandler{

	private final DebugEventCollector simpleDebugEventCollector = SimpleDebuggerEventCollector.instance();
	
	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		UIEvent<InnerElementRepresentationDTO> userRequestedAdditionalInfo = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
		provideAdditionalInfoAboutObject(userRequestedAdditionalInfo);
		return false;
	}
	
	private void provideAdditionalInfoAboutObject(UIEvent<InnerElementRepresentationDTO> userRequestedAdditionalInfo) {
		InnerElementRepresentationDTO anchorElement = userRequestedAdditionalInfo.getPayload();
		UniversalElementRepresentation topLevelElement = TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot()
				.get(anchorElement.getTag());
		Set<UniversalElementRepresentation> relevantElements = compileAdditionalInfo(topLevelElement);
		relevantElements.remove(topLevelElement);
		List<UniversalElementRepresentation> elements = new ArrayList<UniversalElementRepresentation>(relevantElements);
		Collections.sort(elements);
		Map<Integer, ArrayList<UserInstanceInnerElementInspectionDTO>> separatedIntoGroups = Map.of(1,
				new ArrayList<UserInstanceInnerElementInspectionDTO>(), 2,
				new ArrayList<UserInstanceInnerElementInspectionDTO>(), 3,
				new ArrayList<UserInstanceInnerElementInspectionDTO>());
		for (UniversalElementRepresentation element : elements) {
			UserInstanceInnerElementInspectionDTO userInstanceInnerElementInspectionDTO = new UserInstanceInnerElementInspectionDTO(
					element.getElementName(), element.gettypeOrReturnType(), element.getValue());
			if (element.getElementType().ordinal() < 5)
				separatedIntoGroups.get(1).add(userInstanceInnerElementInspectionDTO);
			else if (element.getElementType().ordinal() == 5)
				separatedIntoGroups.get(2).add(userInstanceInnerElementInspectionDTO);
			else if (element.getElementType().ordinal() > 5)
				separatedIntoGroups.get(3).add(userInstanceInnerElementInspectionDTO);
		}
		UserInstanceInspectionDTO userInstanceInspectionDTO = new UserInstanceInspectionDTO(
				topLevelElement.getElementName(), anchorElement.getTypeOrReturnType(), separatedIntoGroups);
		simpleDebugEventCollector.collectDebugEvent(new DebugEvent<UserInstanceInspectionDTO>(
				SimpleDebuggerEventType.DISPLAY_ADDITIONAL_INFO, userInstanceInspectionDTO));

	}
	
	private Set<UniversalElementRepresentation> compileAdditionalInfo(UniversalElementRepresentation topLevelElement) {
		Set<UniversalElementRepresentation> selectedElements = new HashSet<UniversalElementRepresentation>();
		TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().values().stream().filter(e -> Objects.nonNull(e))
				.filter(e -> Objects.equals(e.getElementName(), topLevelElement.getElementName())).findAny()
				.ifPresent(elementName -> {
					elementName.getTag().getUniqueId();
					TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().values().stream()
							.filter(e -> Objects.equals(e.getElementName(), topLevelElement.getElementName())).findAny()
							.ifPresent(field -> {
								TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().values().stream().filter(
										e -> Objects.equals(e.getTag().getParentId(), field.getTag().getUniqueId()))
										.findAny().ifPresent(root -> {
											boolean found = true;
											Set<UniversalElementRepresentation> iterationElements = new HashSet<UniversalElementRepresentation>();
											iterationElements.add(root);
											while (found) {
												for (UniversalElementRepresentation iterationElement : iterationElements) {
													iterationElements.addAll(TargetApplicationRepresentation.getInstance()
															.getTargetApplicationSnapshot().values().stream()
															.filter(e -> Objects.equals(e.getObjectReference(),
																	iterationElement.getObjectReference()))
															.filter(e -> !Objects.equals(e.getAdditionalInfo(),
																	topLevelElement.gettypeOrReturnType()))
															.toList());
												}
												iterationElements.remove(root);
												if (iterationElements.isEmpty())
													found = false;
												selectedElements.addAll(iterationElements);
												iterationElements.clear();
											}
										});
							});
				});
		return selectedElements;
	}
}
