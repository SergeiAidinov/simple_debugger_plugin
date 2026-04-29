package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class UserRequestedCollectionPageHandler implements UIEventHandler{
	
	 private final TreeMap<Integer, InnerElementRepresentationDTO> collectionElements = new TreeMap<>();
	 private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();
	 private BreakpointEvent breakpointEvent;
	 private String collectionType;

	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		this.breakpointEvent = breakpointEvent;
		UIEvent<PairDTO<Long, Integer>> uiEvent = null;
		try {
			uiEvent = (UIEvent<PairDTO<Long, Integer>>) abstractSimpleDebuggerUIEvent;
		} catch (ClassCastException castException) {

		}
		final long id = uiEvent.getPayload().getFirst();
		Map<Long, UniversalElementRepresentation> map = TargetApplicationRepresentation.getInstance().getAllElements()
				.stream().filter(UniversalElementRepresentation.class::isInstance)
				.map(UniversalElementRepresentation.class::cast).filter(e -> e.getObjectReference() != null)
				.collect(Collectors.toMap(e -> (Long) e.getObjectReference().uniqueID(), Function.identity(),
						(existing, duplicate) -> existing));
		
		UniversalElementRepresentation collectionRepresentation = map.get(id);
		
		Optional<UniversalElementRepresentation> collectionrepresentationOptional = TargetApplicationRepresentation.getInstance().getAllElements()
				.stream()
				.filter(e -> e instanceof UniversalElementRepresentation)
				.map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.nonNull(e.getObjectReference()))
				.filter(e -> Objects.equals(e.getObjectReference().uniqueID(), id)).findAny();
		if (collectionrepresentationOptional.isEmpty()) return false;
		int collectionSize = 0;
		if (Objects.nonNull(collectionRepresentation)) {
			String mapSizeString = collectionRepresentation.getValue().substring(collectionRepresentation.getValue().indexOf(':') + 1,
					collectionRepresentation.getValue().indexOf(','));
			System.out.println(mapSizeString);
			try {
				collectionSize = Integer.valueOf(mapSizeString);
			} catch (NumberFormatException e) {
				// TODO: handle exception
			}
		}
		int totalEntries = collectionSize;
		int totalPages = (totalEntries + DebugUtils.PAGE_SIZE - 1) / DebugUtils.PAGE_SIZE;
		// int pageNumber = uiEvent.getPayload().getSecond();

		int fromIndex = uiEvent.getPayload().getSecond() * DebugUtils.PAGE_SIZE;
		int toIndex = fromIndex + DebugUtils.PAGE_SIZE - 1;
		
		  List<Value> qq = DebugUtils.iterateThroughCollection(collectionRepresentation.getObjectReference(), breakpointEvent);
		  Map<Integer, UniversalElementRepresentation> collectionElements = new TreeMap<Integer, UniversalElementRepresentation>();

		  for (int i = 0; i < qq.size(); i++) {
			  Value value = qq.get(i);
		      if (value == null) continue;

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
		                  .elementName("item")
		                  .value(value.toString())
		                  .valueCategory(UniversalElementRepresentation.ValueCategory.PRIMITIVE)
		                  .build();
		      }

		      collectionElements.put(i, element);
		  }
		  
		 ArrayPageDTO page = ArrayPageDTO.builder()
          .elementName(collectionRepresentation.getElementName())
          .elementType(collectionRepresentation.getElementType().name()) // преобразуем enum в строку для DTO
          .totalElements(String.valueOf(collectionSize))
          .currentPage(uiEvent.getPayload().getSecond())
          .totalPages(String.valueOf(totalPages))
          .fromIndex(fromIndex)
          .toIndex(toIndex)
          .entries(collectionElements)
          .anchorTag(collectionRepresentation.getTag())
          .build();
		 
		 debugEventCollector.collectDebugEvent(
					new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_ITERABLE, page));
		
//
//		UniversalElementRepresentation mapRepresentation = map.get(id);	
//		compileCollectionElements(InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(mapRepresentation));
		
		
		
		
		System.out.println(id);
		
		
		
		
		return false;
	}
	
	 private void compileCollectionElements(InnerElementRepresentationDTO anchorElement) {
	        Optional<UniversalElementRepresentation> collectionOpt = TargetApplicationRepresentation.getInstance()
	                .getAllElements()
	                .stream()
	                .filter(e -> e instanceof UniversalElementRepresentation)
	                .map(e -> (UniversalElementRepresentation) e)
	                .filter(e -> Objects.equals(e.getTag(), anchorElement.getTag()))
	                .findAny();

	        if (collectionOpt.isEmpty()) return;

	        UniversalElementRepresentation collection = collectionOpt.get();

	        TripletDTO<String, String, String> ww = DebugUtils.determinCollectionType(
	                collection.getObjectReference(), breakpointEvent
	        );
	        collectionType = ww.getFirst();

	        // Преобразуем строку типа элемента в enum
//	        try {
//	            elementType = UniversalElementType.valueOf(ww.getSecond());
//	        } catch (IllegalArgumentException e) {
//	            elementType = UniversalElementType.UNKNOWN;
//	        }

	        List<Value> values = DebugUtils.iterateThroughCollection(collection.getObjectReference(), breakpointEvent);

	        for (int i = 0; i < values.size(); i++) {
	            Value v = values.get(i);
	            if (v == null) continue;

	            UniversalElementRepresentation uer;
	            if (v instanceof ObjectReference objRef) {
	                String type = objRef.referenceType().name();
	                String valueText = type.startsWith("java.lang.") ? objRef.toString() : type;

	                uer = UniversalElementRepresentation.builder()
	                        .referenceType(objRef.referenceType())
	                        .objectReference(objRef)
	                        .elementName(valueText)
	                        .elementType(UniversalElementType.COLLECTION_ELEMENT)
	                        .currentRole(CurrentRole.INNER)
	                        .value(DebugUtils.getObjectReferenceValueAsString(objRef))
	                        .valueCategory(DebugUtils.determineValueCategory(v))
	                        .uniqueId(UUID.randomUUID())
	                        .parentUniqueId(collection.getTag().getUniqueId())
	                        .level(collection.getLevel() + 1)
	                        .build();
	            } else {
	                uer = UniversalElementRepresentation.builder()
	                        .elementName("item")
	                        .value(v.toString())
	                        .valueCategory(ValueCategory.PRIMITIVE)
	                        .uniqueId(UUID.randomUUID())
	                        .parentUniqueId(collection.getTag().getUniqueId())
	                        .level(collection.getLevel() + 1)
	                        .build();
	            }

	            InnerElementRepresentationDTO dto = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(uer);
	            collectionElements.put(i, dto);
	        }
	    }
	
	

}
