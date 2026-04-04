package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable;

import java.util.*;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class InspectableInstanceElement extends AbstractInspectableElement {

	private final BreakpointEvent breakpointEvent;
	private final InnerElementRepresentationDTO anchorElement;

	private String classType = "";

	private final List<InnerElementRepresentationDTO> fields = new ArrayList<>();

	public InspectableInstanceElement(InnerElementRepresentationDTO anchorElement, BreakpointEvent breakpointEvent) {
		super(anchorElement.getTag(), anchorElement.getElementName(), anchorElement.getElementType(),
				anchorElement.getValueCategory(), true);
		this.breakpointEvent = breakpointEvent;
		this.anchorElement = anchorElement;

		compileFieldsAndMethods();
	}

	public String getClassType() {
		return classType;
	}

	public List<InnerElementRepresentationDTO> getFields() {
		return fields;
	}

	public InnerElementRepresentationDTO getAnchorElement() {
		return anchorElement;
	}

	private void compileFieldsAndMethods() {
		Optional<UniversalElementRepresentation> qq = TargetApplicationRepresentation.getInstance().getAllElements()
				.stream().filter(e -> e instanceof UniversalElementRepresentation)
				.map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getTag(), anchorElement.getTag())).findAny();
		
		if (qq.isPresent()) {
			classType = qq.get().getAdditionalInfo();
			List<UniversalElementRepresentation> ww = TargetApplicationRepresentation.getInstance().getAllElements()
					.stream().filter(e -> e instanceof UniversalElementRepresentation)
					.map(e -> (UniversalElementRepresentation) e)
					.filter(e -> Objects.equals(e.getTag().getParentId(), qq.get().getTag().getUniqueId())).toList();
			for (UniversalElementRepresentation universalElementRepresentation : ww) {
				InnerElementRepresentationDTO innerElementRepresentationDTO = 
					InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(universalElementRepresentation);
				fields.add(innerElementRepresentationDTO);
			}
			System.out.println(ww);
		}
	}

	public UserObjectPageDTO inspectPage(InspectableInstanceElement element) {
	    return UserObjectPageDTO.builder()
	            .anchorTag(element.getTag())
	            .elementName(element.getElementName())
	            .elementType(element.getElementType().name())
	            .classType(classType)  // <-- сюда classType
	            .entries(fields)
	            .build();
	}
	
}