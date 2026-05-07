package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.Objects;
import java.util.Optional;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider.MapDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.DataProviderHolderImpl;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

public class UserRequestedMapPageHandler implements UIEventHandler {

	@SuppressWarnings("unchecked")
	@Override
	public boolean handle(HandlerContext abstractUIEventContext, AbstractUIEvent abstractSimpleDebuggerUIEvent) {
		InspectionHandlerContext inspectionHandlerContext =  (InspectionHandlerContext) abstractUIEventContext;
		// UIEvent<T> uiEvent = (UIEvent<T>) abstractSimpleDebuggerUIEvent;
		UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>> userReqeustedMapPageEvent = null;
		try {
			userReqeustedMapPageEvent = (UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>>) abstractSimpleDebuggerUIEvent;
		} catch (ClassCastException castException) {
			System.out.println(castException);
		}

		final long id = userReqeustedMapPageEvent.getPayload().getFirst().getObjectId();
		String descriprion = userReqeustedMapPageEvent.getPayload().getFirst().getElementName() + " page: "
				+ userReqeustedMapPageEvent.getPayload().getSecond();
		System.out.println(descriprion);
		inspectionHandlerContext.getInspectionSeanceCache().addBreadCrumbIfNecessary(
				userReqeustedMapPageEvent.getPayload().getFirst().getObjectId(), userReqeustedMapPageEvent,
				descriprion, userReqeustedMapPageEvent.getPayload().getSecond());
		DataProviderHolderImpl dataProviderHolder = inspectionHandlerContext.getInspectionSeanceCache()
				.getDataProviderHolders().get(id);
		// MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> page
		// = null;
		if (Objects.nonNull(dataProviderHolder)) {
			dataProviderHolder.handleEvent(userReqeustedMapPageEvent);
			System.out.println();
		} else {
			Optional<UniversalElementRepresentation> i = TargetApplicationRepresentation.getInstance().getAllElements()
					.stream().filter(e -> e instanceof UniversalElementRepresentation)
					.map(e -> (UniversalElementRepresentation) e)
					.filter(e -> Objects.equals(e.getObjectReferenceId(), id)).findAny();

			UniversalElementRepresentation mapRepresentation = i.get();
			DataProvider mapDataProvider = new MapDataProvider(mapRepresentation, inspectionHandlerContext);
			dataProviderHolder = new DataProviderHolderImpl(mapDataProvider,
					DebuggerContext.context().getInspectionSeanceId());
			dataProviderHolder.start();
			inspectionHandlerContext.getInspectionSeanceCache().getDataProviderHolders().put(id,
					dataProviderHolder);
			System.out.println(userReqeustedMapPageEvent);
			inspectionHandlerContext.getInspectionSeanceCache().getDataProviderHolders().get(id)
					.handleEvent(userReqeustedMapPageEvent);

		}
//		debugEventCollector.collectDebugEvent(new DebugEvent<>(
//				SimpleDebuggerEventTypes.SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_MAP, page));
		return false;
	}
}

//		Map<Long, UniversalElementRepresentation> map = TargetApplicationRepresentation.getInstance().getAllElements()
//				.stream().filter(UniversalElementRepresentation.class::isInstance)
//				.map(UniversalElementRepresentation.class::cast).filter(e -> e.getObjectReference() != null)
//				.collect(Collectors.toMap(e -> (Long) e.getObjectReference().uniqueID(), Function.identity(),
//						(existing, duplicate) -> existing));
//		Optional<UniversalElementRepresentation> i = TargetApplicationRepresentation.getInstance().getAllElements()
//				.stream().filter(e -> e instanceof UniversalElementRepresentation)
//				.map(e -> (UniversalElementRepresentation) e).filter(e -> Objects.equals(e.getObjectReferenceId(), id))
//				.findAny();
//
//		UniversalElementRepresentation mapRepresentation = i.get();
//
//		// map.get(id);
//
//		int mapSize = 0;
//		if (Objects.nonNull(mapRepresentation)) {
//			String mapSizeString = mapRepresentation.getValue().substring(mapRepresentation.getValue().indexOf(':') + 1,
//					mapRepresentation.getValue().indexOf(','));
//			System.out.println(mapSizeString);
//			try {
//				mapSize = Integer.valueOf(mapSizeString);
//			} catch (NumberFormatException e) {
//				// TODO: handle exception
//			}
//		}
//		int totalEntries = mapSize;
//		int totalPages = (totalEntries + DebugUtils.PAGE_SIZE - 1) / DebugUtils.PAGE_SIZE;
//		// int pageNumber = uiEvent.getPayload().getSecond();
//
//		int fromIndex = uiEvent.getPayload().getSecond() * DebugUtils.PAGE_SIZE;
//		int toIndex = fromIndex + DebugUtils.PAGE_SIZE - 1;
//
//		List<Entry<Value, Value>> pageEntries = DebugUtils.iterateThroughMap(mapRepresentation.getObjectReference(),
//				breakpointEvent, fromIndex, toIndex);
////		List<Entry<Value, Value>> pageEntries1 = DebugUtils.iterateThroughMap(mapRepresentation.getObjectReference(),
////				breakpointEvent);
//		long l = System.currentTimeMillis();
////		List<Entry<Value, Value>> qqq = MapDataProvider.iterateThroughMap(mapRepresentation.getObjectReference(),
////				breakpointEvent, 0, 16000);
//		System.out.println("TINE: " + (System.currentTimeMillis() - l));
//		Map<UniversalElementRepresentation, UniversalElementRepresentation> qq = new HashMap<UniversalElementRepresentation, UniversalElementRepresentation>();
//		Map<UniversalElementRepresentation, UniversalElementRepresentation> collectionElements = new HashMap<>();
//		for (Entry<Value, Value> entry : pageEntries) {
//			Value keyValue = entry.getKey();
//			Value valueValue = entry.getValue();
//			UniversalElementRepresentation keyElement = createUniversalElementRepresentationFromValue(keyValue, map);
//			UniversalElementRepresentation valueElement = createUniversalElementRepresentationFromValue(valueValue,
//					map);
//			// InspectionSeance.inspectionSeanceCache.put(valueElement.getObjectReference().uniqueID(),
//			// valueElement);
//			collectionElements.put(keyElement, valueElement);
//		}

//		List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> list = new ArrayList<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>>();
//		for (Entry<UniversalElementRepresentation, UniversalElementRepresentation> entry : collectionElements
//				.entrySet()) {
//			PairDTO<UniversalElementRepresentation, UniversalElementRepresentation> e = PairDTO.of(entry.getKey(),
//					entry.getValue());
//			list.add(PairDTO.of(
//					InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e.getFirst()),
//					InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e.getSecond())));
//
//		}
//		MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> page = MapPageDTO
//				.<InnerElementRepresentationDTO, InnerElementRepresentationDTO>builder()
//				.anchorMap(uiEvent.getPayload().getFirst()).elementName(mapRepresentation.getElementName())
//				.elementType(mapRepresentation.getAdditionalInfo()).totalEntries(totalEntries)
//				.currentPage(uiEvent.getPayload().getSecond()).totalPages(totalPages).fromIndex(fromIndex)
//				.toIndex(toIndex).entries(list).anchorTag(mapRepresentation.getTag()).build();
//		debugEventCollector.collectDebugEvent(new DebugEvent<>(
//				SimpleDebuggerEventTypes.SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_MAP, page));

//		return false;
//	}

//	private UniversalElementRepresentation createUniversalElementRepresentationFromValue(Value value,
//			Map<Long, UniversalElementRepresentation> map) {
//		UniversalElementRepresentation element = null;
//		if (value instanceof ObjectReference objRef) {
//			element = map.get(objRef.uniqueID());
//
//			// 🔥 ВАЖНО: fallback
//			if (element == null) {
//				String type = objRef.referenceType().name();
//				String valueText = type.startsWith("java.lang.") ? objRef.toString() : type;
//
//				element = UniversalElementRepresentation.builder().referenceType(objRef.referenceType())
//						.objectReference(objRef).elementName(valueText)
//						.elementType(UniversalElementRepresentation.UniversalElementType.COLLECTION_ELEMENT)
//						.currentRole(UniversalElementRepresentation.CurrentRole.INNER)
//						.value(DebugUtils.getObjectReferenceValueAsString(objRef))
//						.valueCategory(DebugUtils.determineValueCategory(value)).build();
//			}
//		} else {
//			element = UniversalElementRepresentation.builder().elementName(value.toString())
//					.valueCategory(UniversalElementRepresentation.ValueCategory.PRIMITIVE).build();
//		}
//
//		return element;
//
//	}
