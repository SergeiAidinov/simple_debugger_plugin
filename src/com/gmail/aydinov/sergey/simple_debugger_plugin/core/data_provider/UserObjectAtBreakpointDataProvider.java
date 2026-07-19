package com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider.PageableDataProvider;
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

public class UserObjectAtBreakpointDataProvider implements PageableDataProvider {

	private final InnerElementRepresentationDTO innerElementRepresentationDTO;
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();
	private final InspectionHandlerContext inspectionHandlerContext;
	private UserObjectPageDTO cachedUserObjectPageDTO;
	private long dataProviderHolderId;

	public UserObjectAtBreakpointDataProvider(InnerElementRepresentationDTO innerElementRepresentationDTO,
			HandlerContext handlerContext) {
		this.innerElementRepresentationDTO = innerElementRepresentationDTO;
		this.inspectionHandlerContext = (InspectionHandlerContext) handlerContext;
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
		
		InnerElementRepresentationDTO userObject = innerElementRepresentationDTO;
		List<UniversalElementRepresentation> uObjs = findAllUserObjects(userObject);
		if (uObjs.isEmpty()) {
		}
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
					.filter(e -> Objects.equals(e.getTag().getParentId(), classRepresentation.getTag().getUniqueId()))
					.toList().stream()
					.map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e))
					.toList());
		}
		List<InnerElementRepresentationDTO> subordinates = new ArrayList<InnerElementRepresentationDTO>(fields);
		subordinates.addAll(methods);
		subordinates = subordinates.stream().distinct().toList();
		UserObjectPageDTO userObjectPageDTO = UserObjectPageDTO.builder().elementName(userObject.getElementName())
				.elementType(userObject.getTypeOrReturnType()).objectId(userObject.getObjectId()).entries(subordinates)
				.classType(userObject.getTypeOrReturnType()).anchorTag(userObject.getTag()).build();
		
	//	UserObjectPageDTO qq = build(uObjs.get(0).getObjectReference());
		return userObjectPageDTO;

	}

	private List<UniversalElementRepresentation> findAllUserObjects(InnerElementRepresentationDTO anchor) {
		TargetApplicationRepresentation.getInstance().getAllElements().stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> e.getObjectReference() != null).forEach(e -> {
					if (Objects.equals(e.getObjectReference().uniqueID(), anchor.getObjectId())) {
						System.out.println("FOUND MATCH: " + e);
					}
				});
		List<UniversalElementRepresentation> userObjects = new ArrayList<UniversalElementRepresentation>();
		userObjects.addAll(TargetApplicationRepresentation.getInstance().getAllElements().stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.nonNull(e.getObjectReference()))
				.filter(e -> Objects.equals(e.getObjectReference().uniqueID(), anchor.getObjectId())).toList());
		if (!userObjects.isEmpty())
			return userObjects;

		userObjects.addAll(TargetApplicationRepresentation.getInstance().getAllElements().stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.nonNull(e.getObjectReference()))
				.filter(e -> Objects.equals(anchor.getTag(), e.getTag())).toList());
		
			return userObjects;
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
			String params = String.join(", ", method.argumentTypeNames());
			UniversalElementRepresentation methodElement = UniversalElementRepresentation.builder().objectReference(obj)
					.elementType(UniversalElementType.METHOD).value(method.toString())
					.typeOrReturnType(method.returnTypeName()).isStatic(method.isStatic()).parentUniqueId(rootId)
					.level(1).build();
			entries.add(methodElement);
		}
		List<InnerElementRepresentationDTO> qq = entries.stream()
				.map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e)).toList();
		UserObjectPageDTO userObjectPageDTO = UserObjectPageDTO.builder().elementName(obj.type().name())
				.elementType(obj.type().name()).classType(type.name()).entries(qq).anchorTag(new Tag(rootId, null))
				.objectId(obj.uniqueID()).build();

		return userObjectPageDTO;
	}
	
//	@Override
//	public void setDataProviderHolderId(long id) {
//		dataProviderHolderId = id;
//		
//	}

	@Override
	public long getDataProviderHolderId() {
		return dataProviderHolderId;
	}

}
