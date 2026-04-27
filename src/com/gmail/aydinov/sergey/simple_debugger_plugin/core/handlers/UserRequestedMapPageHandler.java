package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class UserRequestedMapPageHandler implements UIEventHandler {

	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();

	@SuppressWarnings("unchecked")
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
		Optional<UniversalElementRepresentation> i = TargetApplicationRepresentation.getInstance().getAllElements().stream()
		.filter(e -> e instanceof UniversalElementRepresentation)
		.map(e -> (UniversalElementRepresentation) e)
		.filter(e -> Objects.equals(e.getObjectReferenceId(), id)).findAny();
		
		UniversalElementRepresentation mapRepresentation = i.get();
				
			//	map.get(id);
		
		int mapSize = 0;
		if (Objects.nonNull(mapRepresentation)) {
			String mapSizeString = mapRepresentation.getValue().substring(mapRepresentation.getValue().indexOf(':') + 1,
					mapRepresentation.getValue().indexOf(','));
			System.out.println(mapSizeString);
			try {
				mapSize = Integer.valueOf(mapSizeString);
			} catch (NumberFormatException e) {
				// TODO: handle exception
			}
		}
		int totalEntries = mapSize;
		int totalPages = (totalEntries + DebugUtils.PAGE_SIZE - 1) / DebugUtils.PAGE_SIZE;
		// int pageNumber = uiEvent.getPayload().getSecond();

		int fromIndex = uiEvent.getPayload().getSecond() * DebugUtils.PAGE_SIZE;
		int toIndex = fromIndex + DebugUtils.PAGE_SIZE - 1;

		List<Entry<Value, Value>> pageEntries = DebugUtils.iterateThroughMap(mapRepresentation.getObjectReference(),
				breakpointEvent, fromIndex, toIndex);
		Map<UniversalElementRepresentation, UniversalElementRepresentation> qq = new HashMap<UniversalElementRepresentation, UniversalElementRepresentation>();
		 Map<UniversalElementRepresentation, UniversalElementRepresentation> collectionElements = new HashMap<>();  
		for (Entry<Value, Value> entry : pageEntries) {
			  Value keyValue = entry.getKey();
			  Value valueValue = entry.getValue();
		      UniversalElementRepresentation keyElement = createUniversalElementRepresentationFromValue(keyValue, map);
		      UniversalElementRepresentation valueElement = createUniversalElementRepresentationFromValue(valueValue, map);

			  collectionElements.put(keyElement, valueElement);
		  }

		List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> list = new ArrayList<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>>();
		for (Entry<UniversalElementRepresentation, UniversalElementRepresentation> entry : collectionElements.entrySet()) {
			PairDTO<UniversalElementRepresentation, UniversalElementRepresentation> e = PairDTO.of(entry.getKey(),
					entry.getValue());
			list.add(PairDTO.of(
					InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e.getFirst()),
					InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e.getSecond())));

		}
		MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> page = MapPageDTO
				.<InnerElementRepresentationDTO, InnerElementRepresentationDTO>builder()
				.elementName(mapRepresentation.getElementName()).elementType(mapRepresentation.getAdditionalInfo())
				.totalEntries(totalEntries).currentPage(uiEvent.getPayload().getSecond()).totalPages(totalPages)
				.fromIndex(fromIndex).toIndex(toIndex).entries(list).anchorTag(mapRepresentation.getTag()).build();
		debugEventCollector.collectDebugEvent(new DebugEvent<>(
				SimpleDebuggerEventTypes.SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_MAP, page));

		return false;
	}
	
	private UniversalElementRepresentation createUniversalElementRepresentationFromValue(Value value, Map<Long, UniversalElementRepresentation> map) {
		UniversalElementRepresentation element = null;
		if (value instanceof ObjectReference objRef) {
	          element = map.get(objRef.uniqueID());

	          // 🔥 ВАЖНО: fallback
	          if (element == null) {
	              String type = objRef.referenceType().name();
	              String valueText = type.startsWith("java.lang.") ? objRef.toString() : type;

	              element = UniversalElementRepresentation.builder()
	                      .referenceType(objRef.referenceType())
	                      .objectReference(objRef)
	                      .elementName(valueText)
	                      .elementType(UniversalElementRepresentation.UniversalElementType.COLLECTION_ELEMENT)
	                      .currentRole(UniversalElementRepresentation.CurrentRole.INNER)
	                      .value(DebugUtils.getObjectReferenceValueAsString(objRef))
	                      .valueCategory(DebugUtils.determineValueCategory(value))
	                      .build();
	          }
	      } else {
	          element = UniversalElementRepresentation.builder()
	                  .elementName(value.toString())
	                  .valueCategory(UniversalElementRepresentation.ValueCategory.PRIMITIVE)
	                  .build();
	      }
		
		return element;
		
	}

//	public AbstractInspectionCollectionPage<?> inspectPage(AbstractInspectableElement inspectableElement,
//			int pageNumber) {
//		if (!(inspectableElement instanceof InspectableMapElement map))
//			throw new IllegalArgumentException("Expected InspectableMapElement");
//		Optional<UniversalElementRepresentation> mapRepresentation = TargetApplicationRepresentation.getInstance().getAllElements()
//				.stream().filter(e -> Objects.equals(e.getTag(), anchorElement.getTag()))
//				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
//				.findAny();
//		int mapSize = 0;
//		if (mapRepresentation.isPresent()) {
//			String mapSizeString = mapRepresentation.get().getValue().substring(mapRepresentation.get().getValue().indexOf(':') + 1,
//					mapRepresentation.get().getValue().indexOf(','));
//			System.out.println(mapSizeString);
//			try {
//				mapSize = Integer.valueOf(mapSizeString);
//			} catch (NumberFormatException e) {
//				// TODO: handle exception
//			}
//		}
//		int totalEntries = mapSize;
//		int totalPages = (totalEntries + DebugUtils.PAGE_SIZE - 1) / DebugUtils.PAGE_SIZE;
//
//		int fromIndex = pageNumber * DebugUtils.PAGE_SIZE;
//		int toIndex = Math.min(fromIndex + DebugUtils.PAGE_SIZE - 1, totalEntries - 1);
//
//		List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> pageEntries = map
//				.getPage(pageNumber);
//
//		return MapPageDTO.<InnerElementRepresentationDTO, InnerElementRepresentationDTO>builder()
//				.elementName(map.getAnchorElement().getElementName()).elementType(map.getMapType())
//				.totalEntries(totalEntries).currentPage(pageNumber).totalPages(totalPages).fromIndex(fromIndex)
//				.toIndex(toIndex).entries(pageEntries).anchorTag(map.getAnchorElement().getTag()).build();
//	}

}
