package com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;

public class UserObjectInspectionDataProvider implements DataProvider {
	
	private final InnerElementRepresentationDTO innerElementRepresentationDTO;
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();
	private final InspectionHandlerContext inspectionHandlerContext;
	private UserObjectPageDTO cachedUserObjectPageDTO;
	
	

	public UserObjectInspectionDataProvider(InnerElementRepresentationDTO innerElementRepresentationDTO,
			InspectionHandlerContext inspectionHandlerContext) {
		this.innerElementRepresentationDTO = innerElementRepresentationDTO;
		this.inspectionHandlerContext = inspectionHandlerContext;
	}

	@Override
	public void handlePageRequest(Integer pageNumber) {
		System.out.println("OBJECT REQUEST");
		if (Objects.isNull(cachedUserObjectPageDTO))
			cachedUserObjectPageDTO = cachePage();
		if (Objects.nonNull(cachedUserObjectPageDTO)) {
			cachedUserObjectPageDTO
					.setBreadcrumbs(inspectionHandlerContext.getInspectionSeanceCache().groupBreadCrumbsintoPairs());
			debugEventCollector.collectDebugEvent(new DebugEvent<>(
					SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_USER_OBJECT, cachedUserObjectPageDTO));
		}
		
	}

	private UserObjectPageDTO cachePage() {
		ObjectReference objectReference = inspectionHandlerContext.getInspectionSeanceCache().getLoadedPieces().get(innerElementRepresentationDTO.getObjectId());
		cachedUserObjectPageDTO = build(objectReference);
		return cachedUserObjectPageDTO;
	}

	
	private UserObjectPageDTO build(ObjectReference obj) {

		ReferenceType type = obj.referenceType();

		// ===== ROOT OBJECT =====

		UUID rootId = UUID.randomUUID();
		List<UniversalElementRepresentation> entries = new ArrayList<UniversalElementRepresentation>();

		// =========================================================
		// FIELDS (instance data)
		// =========================================================
		for (Field field : type.allFields()) {
			try {
				Value value = obj.getValue(field);

				UniversalElementRepresentation fieldElement = UniversalElementRepresentation.builder()
						.objectReference(obj).elementName(field.name()).additionalInfo(field.typeName())
						.elementType(UniversalElementType.FIELD).value(String.valueOf(value)).isStatic(field.isStatic())
						.valueCategory(DebugUtils.determineValueCategory(value)).typeOrReturnType(field.typeName())
						.parentUniqueId(rootId).level(1).build();
				entries.add(fieldElement);
			} catch (Exception ignored) {
			}
		}

		// =========================================================
		// METHODS (class behavior)
		// =========================================================
		for (Method method : type.methods()) {
			if (DebugUtils.shouldSkipMethod(method))
				continue;
			String returnValue = method.returnTypeName();
			String params = String.join(", ", method.argumentTypeNames());
			UniversalElementRepresentation methodElement = UniversalElementRepresentation.builder().objectReference(obj)
					.elementName(method.name() + "()")
					.elementType(UniversalElementType.METHOD).value(returnValue + " " + method.name() + "(" + params + ")")
					.typeOrReturnType(method.returnTypeName()).isStatic(method.isStatic()).parentUniqueId(rootId)
					.level(1).build();
			entries.add(methodElement);
		}
		List<InnerElementRepresentationDTO> dtoEntries = entries.stream()
				.map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e)).toList();
		UserObjectPageDTO userObjectPageDTO = UserObjectPageDTO.builder().elementName(obj.type().name())
				.elementType(obj.type().name()).classType(type.name()).entries(dtoEntries).anchorTag(new Tag(rootId, null))
				.objectId(obj.uniqueID()).build();
		return userObjectPageDTO;
	}

}
