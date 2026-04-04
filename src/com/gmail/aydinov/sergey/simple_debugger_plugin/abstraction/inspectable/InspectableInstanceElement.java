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

	private String classType;

	private final List<PairDTO<String, Object>> fields = new ArrayList<>();
	private final List<PairDTO<String, String>> methods = new ArrayList<>();

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

	public List<PairDTO<String, Object>> getFields() {
		return fields;
	}

	public List<PairDTO<String, String>> getMethods() {
		return methods;
	}

	public InnerElementRepresentationDTO getAnchorElement() {
		return anchorElement;
	}

	private void compileFieldsAndMethods() {
		ObjectReference objRef = null;
		Optional<UniversalElementRepresentation> qq = TargetApplicationRepresentation.getInstance().getAllElements()
				.stream().filter(e -> e instanceof UniversalElementRepresentation)
				.map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getTag(), anchorElement.getTag())).findAny();
		if (qq.isPresent()) objRef = qq.get().getObjectReference();

		if (objRef == null)
			return;

		ReferenceType refType = objRef.referenceType();
		classType = refType != null ? refType.name() : "Unknown";

		// ===== Поля =====
		for (Field field : refType.allFields()) {
			Value value = objRef.getValue(field);
			InnerElementRepresentationDTO fieldDto = DebugUtils.createInnerElementDTO(value,
					UniversalElementRepresentation.builder().referenceType(refType)
							.objectReference(value instanceof ObjectReference ? (ObjectReference) value : null)
							.elementName(field.name()).elementType(UniversalElementType.FIELD)
							.currentRole(CurrentRole.INNER).value(DebugUtils.valueToString(value))
							.valueCategory(DebugUtils.determineValueCategory(value)).uniqueId(UUID.randomUUID())
							.parentUniqueId(anchorElement.getTag().getUniqueId()).level(anchorElement.getLevel() + 1)
							.build(),
					fields.size());
			fields.add(PairDTO.of(field.name(), fieldDto));
		}

		// ===== Методы =====
		for (Method method : refType.allMethods()) {
			String arguments = String.join(", ", method.argumentTypeNames());
			methods.add(PairDTO.of(method.name(), arguments));
		}
		System.out.println(fields + " " + methods);
	}

	public UserObjectPageDTO inspectPage(InspectableInstanceElement element) {
	    // Собираем все поля как пары <имя, значение>
	   List<PairDTO<String, Object>> fieldEntries = element.getFields();

	    // Методы тоже можно добавить как пары <имя, аргументы>
	    List<PairDTO<String,String>> methodEntries = element.getMethods();

	    // Создаём DTO страницы
	    return UserObjectPageDTO.builder()
	            .anchorTag(element.getTag())
	            .elementName(element.getElementName())
	            .elementType(element.getElementType().name())
	            .classType(element.getClassType())  // <-- сюда classType
	            .entries(fieldEntries)
	            .build();
	}
	
}