package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserElementDetailDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;


public class UserRerquestedAdditionalInfoAboutObjectHandler implements UIEventHandler {

	private final DebugEventCollector simpleDebugEventCollector = SimpleDebuggerEventCollector.instance();

	private static final EnumSet<UniversalElementType> firstGroup = EnumSet.of(UniversalElementType.INTERFACE, UniversalElementType.CLASS,
			UniversalElementType.ENUM, UniversalElementType.FIELD);
	private static final EnumSet<UniversalElementType> secondGroup = EnumSet.of(UniversalElementType.METHOD);

	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		UIEvent<InnerElementRepresentationDTO> userRequestedAdditionalInfo = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
		provideAdditionalInfoAboutObject(userRequestedAdditionalInfo);
		return false;
	}

	private void provideAdditionalInfoAboutObject(UIEvent<InnerElementRepresentationDTO> userRequestedAdditionalInfo) {
		InnerElementRepresentationDTO anchorElement = userRequestedAdditionalInfo.getPayload();
		Optional<UniversalElementRepresentation> topLevelElementOptional = TargetApplicationRepresentation.getInstance()
				.getAllElements().stream().filter(e -> Objects.equals(e.getTag(), anchorElement.getTag()))
				.map(e -> (UniversalElementRepresentation) e).findAny();
		if (topLevelElementOptional.isEmpty())
			return;
		UniversalElementRepresentation topLevelElement = topLevelElementOptional.get();
		Set<UniversalElementRepresentation> relevantElements = compileAdditionalInfo(topLevelElement);
		relevantElements.remove(topLevelElement);
		List<UniversalElementRepresentation> elements = new ArrayList<UniversalElementRepresentation>(relevantElements);
		Collections.sort(elements);
		Map<Integer, ArrayList<UserElementDetailDTO>> separatedIntoGroups = Map.of(1,
				new ArrayList<UserElementDetailDTO>(), 2,
				new ArrayList<UserElementDetailDTO>(), 3,
				new ArrayList<UserElementDetailDTO>());
		for (UniversalElementRepresentation element : elements) {
			UserElementDetailDTO userInstanceInnerElementInspectionDTO = new UserElementDetailDTO(
					element.getElementName(), element.getTypeOrReturnType(), element.getValue());
			if (firstGroup.contains(element.getElementType()))
				separatedIntoGroups.get(1).add(userInstanceInnerElementInspectionDTO);
			else if (secondGroup.contains(element.getElementType()))
				separatedIntoGroups.get(2).add(userInstanceInnerElementInspectionDTO);
			else 
				separatedIntoGroups.get(3).add(userInstanceInnerElementInspectionDTO);
		}
		UserInstanceDetailsDTO userInstanceInspectionDTO = new UserInstanceDetailsDTO(
				topLevelElement.getElementName(), anchorElement.getTypeOrReturnType(), separatedIntoGroups);
		simpleDebugEventCollector.collectDebugEvent(new DebugEvent<UserInstanceDetailsDTO>(
				SimpleDebuggerEventType.DISPLAY_ADDITIONAL_INFO, userInstanceInspectionDTO));

	}

	private Set<UniversalElementRepresentation> compileAdditionalInfo(UniversalElementRepresentation topLevelElement) {
		Set<UniversalElementRepresentation> selectedElements = new HashSet<>();
		UniversalElementRepresentation topLevelMatch = findElementByTag(topLevelElement.getTag());
		if (topLevelMatch == null) {
			return selectedElements;
		}
		selectedElements.addAll(compileRelatedElements(topLevelElement));
		return selectedElements;
	}

	private UniversalElementRepresentation findElementByTag(Tag tag) {
		return TargetApplicationRepresentation.getInstance().getAllElements().stream().filter(Objects::nonNull)
				.filter(e -> Objects.equals(e.getTag(), tag)).filter(e -> e instanceof UniversalElementRepresentation)
				.map(e -> (UniversalElementRepresentation) e).findAny().orElse(null);
	}

	private Set<UniversalElementRepresentation> compileRelatedElements(UniversalElementRepresentation topLevelElement) {
		Set<UniversalElementRepresentation> selectedElements = new HashSet<>();
		List<UniversalElementRepresentation> iterationElements = TargetApplicationRepresentation.getInstance()
				.getAllElements().stream().filter(e -> e instanceof UniversalElementRepresentation)
				.filter(e -> Objects.equals(e.getTag().getParentId(), topLevelElement.getTag().getUniqueId()))
				.map(e -> (UniversalElementRepresentation) e).toList();
		selectedElements.addAll(iterationElements);

		Optional<UniversalElementRepresentation> classOfSelectedElementOptional = TargetApplicationRepresentation.getInstance().getAllElements()
				.stream().filter(e -> e instanceof UniversalElementRepresentation)
				.filter(e -> Objects.equals(e.getTag().getUniqueId(), topLevelElement.getTag().getParentId()))
				.map(e -> (UniversalElementRepresentation) e).findAny();

		UniversalElementRepresentation classOfSelectedElement = classOfSelectedElementOptional.get();

		List<UniversalElementRepresentation> methodsOfSelectedElement = TargetApplicationRepresentation.getInstance().getAllElements()
				.stream().filter(e -> e instanceof UniversalElementRepresentation)
				.filter(e -> Objects.equals(e.getTag().getParentId(), classOfSelectedElement.getTag().getUniqueId()))
				.map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getElementType(), UniversalElementType.METHOD)).toList();

		selectedElements.addAll(methodsOfSelectedElement);

		return selectedElements;
	}

}
