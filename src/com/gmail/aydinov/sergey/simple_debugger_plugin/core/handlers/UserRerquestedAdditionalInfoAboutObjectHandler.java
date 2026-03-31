package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.ArrayList;
import java.util.Collections;
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

public class UserRerquestedAdditionalInfoAboutObjectHandler implements UIEventHandler {

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
		Map<Integer, ArrayList<UserInstanceInnerElementInspectionDTO>> separatedIntoGroups = Map.of(1,
				new ArrayList<UserInstanceInnerElementInspectionDTO>(), 2,
				new ArrayList<UserInstanceInnerElementInspectionDTO>(), 3,
				new ArrayList<UserInstanceInnerElementInspectionDTO>());
		for (UniversalElementRepresentation element : elements) {
			UserInstanceInnerElementInspectionDTO userInstanceInnerElementInspectionDTO = new UserInstanceInnerElementInspectionDTO(
					element.getElementName(), element.getTypeOrReturnType(), element.getValue());
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
		Set<UniversalElementRepresentation> selectedElements = new HashSet<>();
		UniversalElementRepresentation topLevelMatch = findElementByTag(topLevelElement.getTag());
		if (topLevelMatch == null) {
			return selectedElements;
		}

//		UniversalElementRepresentation rootChild = findRootChild(topLevelMatch, topLevelElement);
//		if (rootChild == null) {
//			return selectedElements;
//		}

		selectedElements.addAll(iterateRelatedElements(topLevelElement));

		return selectedElements;
	}

	private UniversalElementRepresentation findElementByTag(Tag tag) {
		return TargetApplicationRepresentation.getInstance().getAllElements().stream().filter(Objects::nonNull)
				.filter(e -> Objects.equals(e.getTag(), tag)).filter(e -> e instanceof UniversalElementRepresentation)
				.map(e -> (UniversalElementRepresentation) e).findAny().orElse(null);
	}

	private UniversalElementRepresentation findRootChild(UniversalElementRepresentation parent,
			UniversalElementRepresentation topLevelElement) {
		return TargetApplicationRepresentation.getInstance().getAllElements().stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getTag().getParentId(), parent.getTag().getUniqueId())).findAny()
				.orElse(null);
	}

	private Set<UniversalElementRepresentation> iterateRelatedElements(UniversalElementRepresentation topLevelElement) {
		Set<UniversalElementRepresentation> selectedElements = new HashSet<>();
		List<UniversalElementRepresentation> iterationElements = TargetApplicationRepresentation.getInstance()
				.getAllElements().stream().filter(e -> e instanceof UniversalElementRepresentation)
				.filter(e -> Objects.equals(e.getTag().getParentId(), topLevelElement.getTag().getUniqueId()))
				.map(e -> (UniversalElementRepresentation) e).toList();
		selectedElements.addAll(iterationElements);
		
		Optional<UniversalElementRepresentation> qq = TargetApplicationRepresentation.getInstance()
				.getAllElements().stream().filter(e -> e instanceof UniversalElementRepresentation)
				.filter(e -> Objects.equals(e.getTag().getUniqueId(), topLevelElement.getTag().getParentId()))
				.map(e -> (UniversalElementRepresentation) e).findAny();
		
		UniversalElementRepresentation qqq = qq .get();
		
		 List<UniversalElementRepresentation> mm = TargetApplicationRepresentation.getInstance()
		.getAllElements().stream().filter(e -> e instanceof UniversalElementRepresentation)
		.filter(e -> Objects.equals(e.getTag().getParentId(), qqq.getTag().getUniqueId()))
		.map(e -> (UniversalElementRepresentation) e)
		.filter(e -> Objects.equals(e.getElementType(), UniversalElementType.METHOD))
		.toList();
		
		 selectedElements.addAll(mm);
		
		return selectedElements;
	}

}
