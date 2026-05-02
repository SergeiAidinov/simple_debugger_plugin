package com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionSeance;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.MapEntryDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public final class MapDataProvider implements DataProvider {

	private final UniversalElementRepresentation mapRepresentation;
	private final BreakpointEvent breakpointEvent;

	private final NavigableMap<Integer, Map.Entry<Value, Value>> mapElements = new ConcurrentSkipListMap<>();
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();
	private final AtomicInteger order = new AtomicInteger(0);
	private ThreadReference thread = null;
	private ObjectReference iterator;
	private Method hasNextMethod = null;
	private Method nextMethod = null;
	private Method getMethod = null;
	private ClassType iteratorType = null;
	private Integer pageNumber = null;
	private volatile String totalPages = "calculating...";
	private volatile String totalEntries = "calculating...";

	private InitializationState initState = InitializationState.NOT_STARTED;
	private final AtomicBoolean allElementsLoaded = new AtomicBoolean(false);
	private final AtomicBoolean readingStarted = new AtomicBoolean(false);

	public MapDataProvider(UniversalElementRepresentation mapRepresentation, BreakpointEvent breakpointEvent) {
		this.mapRepresentation = mapRepresentation;
		this.breakpointEvent = breakpointEvent;
	}

	private enum InitializationState {
		NOT_STARTED, IN_PROGRESS, SUCCESS, FAILED
	}

	@Override
	public void requestPage(Integer pageNumber) {
		if (Objects.isNull(pageNumber))
			this.pageNumber = 0;
		else
			this.pageNumber = pageNumber;
		DataProvider.jdiAccessLock.lock();
		try {
			if (initState == InitializationState.NOT_STARTED) {
				initState = InitializationState.IN_PROGRESS;
				if (initiate())
					initState = InitializationState.SUCCESS;
				else
					initState = InitializationState.FAILED;
			}
		} finally {
			DataProvider.jdiAccessLock.unlock();
		}
		DataProvider.jdiAccessLock.lock();
		try {
			if (readingStarted.compareAndSet(false, true))
				iterateThroughMap();
		} finally {
			DataProvider.jdiAccessLock.unlock();
		}
		NavigableMap<Integer, Entry<Value, Value>> selectedItems = waitForPageLoading();

		MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> page = createPageOfMap(selectedItems);
		List<PairDTO<Integer, String>> breadCrumbs = InspectionSeance.getBreadCrumbs();
		page.setBreadcrumbs(breadCrumbs);
		InspectionSeance.lastInspectedAbstractInspectionDTO = page;
		debugEventCollector.collectDebugEvent(new DebugEvent<>(
				SimpleDebuggerEventTypes.SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_MAP, page));
	}

	private void iterateThroughMap() {
		new Thread(() -> {
			while (true) {
				Value hasNextVal = null;
				DataProvider.jdiAccessLock.lock();
				try {
					hasNextVal = iterator.invokeMethod(thread, hasNextMethod, Collections.emptyList(),
							ObjectReference.INVOKE_SINGLE_THREADED);
					if (!(hasNextVal instanceof BooleanValue bv) || !bv.value())
						break;
					Value key = iterator.invokeMethod(thread, nextMethod, Collections.emptyList(),
							ObjectReference.INVOKE_SINGLE_THREADED);
					Value value = mapRepresentation.getObjectReference().invokeMethod(thread, getMethod, List.of(key),
							ObjectReference.INVOKE_SINGLE_THREADED);
					final int index = order.getAndIncrement();
					mapElements.put(index, new AbstractMap.SimpleEntry<>(key, value));
				} catch (Exception e) {
				} finally {
					DataProvider.jdiAccessLock.unlock();
				}
			}
			allElementsLoaded.compareAndSet(false, true);
			totalEntries = String.valueOf(mapElements.size());
			int total = Integer.parseInt(totalEntries);
			int pages = (total + DebugUtils.PAGE_SIZE - 1) / DebugUtils.PAGE_SIZE;
			totalPages = String.valueOf(pages);
		}).start();
	}

	private boolean initiate() {
		ReferenceType refType = mapRepresentation.getObjectReference().referenceType();
		if (!(refType instanceof ClassType classType))
			return false;
		boolean isMap = classType.allInterfaces().stream().anyMatch(iface -> "java.util.Map".equals(iface.name()));
		if (!isMap)
			return false;
		Method getMethod = classType.concreteMethodByName("get", "(Ljava/lang/Object;)Ljava/lang/Object;");
		if (getMethod == null)
			return false;
		this.getMethod = getMethod;
		thread = breakpointEvent.thread();
		Method keySetMethod = classType.concreteMethodByName("keySet", "()Ljava/util/Set;");
		if (keySetMethod == null)
			return false;
		Value keySetValue = null;
		try {
			keySetValue = mapRepresentation.getObjectReference().invokeMethod(thread, keySetMethod,
					Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
		} catch (InvalidTypeException | ClassNotLoadedException | IncompatibleThreadStateException
				| InvocationException e) {
			e.printStackTrace();
		}
		if (!(keySetValue instanceof ObjectReference keySetRef))
			return false;
		ReferenceType keySetType = keySetRef.referenceType();
		if (!(keySetType instanceof ClassType keySetClass))
			return false;
		Method iteratorMethod = keySetClass.concreteMethodByName("iterator", "()Ljava/util/Iterator;");
		if (iteratorMethod == null)
			return false;
		Value iteratorValue = null;
		try {
			iteratorValue = keySetRef.invokeMethod(thread, iteratorMethod, Collections.emptyList(),
					ObjectReference.INVOKE_SINGLE_THREADED);
		} catch (InvalidTypeException | ClassNotLoadedException | IncompatibleThreadStateException
				| InvocationException e) {
			e.printStackTrace();
		}
		if (!(iteratorValue instanceof ObjectReference iterator))
			return false;
		this.iterator = iterator;

		iteratorType = (ClassType) this.iterator.referenceType();
		hasNextMethod = iteratorType.concreteMethodByName("hasNext", "()Z");
		nextMethod = iteratorType.concreteMethodByName("next", "()Ljava/lang/Object;");
		if (hasNextMethod == null || nextMethod == null)
			return false;
		return true;
	}

	private MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> createPageOfMap(
			NavigableMap<Integer, Entry<Value, Value>> selectedItems) {
		List<Integer> sortedIndexes = selectedItems.keySet().stream().sorted().toList();
		Map<MapEntryDTO, MapEntryDTO> collectionElements = new LinkedHashMap<>();
		for (Integer order : sortedIndexes) {
			Value keyValue = selectedItems.get(order).getKey();
			Value valueValue = selectedItems.get(order).getValue();
			if ((keyValue instanceof ObjectReference keyReference)
					&& (valueValue instanceof ObjectReference valueReference)) {
				List<UniversalElementRepresentation> keySubordinates = populateSubordinatesElements(keyReference);
				List<UniversalElementRepresentation> valueSubordinates = populateSubordinatesElements(valueReference);
				List<InnerElementRepresentationDTO> keyEntries = keySubordinates.stream()
						.map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e))
						.toList();
				List<InnerElementRepresentationDTO> valueEntries = valueSubordinates.stream()
						.map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e))
						.toList();
				String keyValueText = DebugUtils.getObjectReferenceValueAsString(keyReference) + " (" + keyReference.type().name() + ", id=" + keyReference.uniqueID() + ")";
				ValueCategory keyCategory = DebugUtils.determineValueCategory(keyValue);
				ValueCategory valueCategory = DebugUtils.determineValueCategory(valueValue);
				String valueValueText = DebugUtils.getObjectReferenceValueAsString(valueReference) + " (id=" + valueReference.uniqueID() + ")";
				collectionElements.put(new MapEntryDTO(keyValueText, keyReference.uniqueID(), keyCategory, keyEntries), new MapEntryDTO(valueValueText, valueReference.uniqueID(), valueCategory, valueEntries));
			}

			// List<UniversalElementRepresentation> valueElement =
			// createUniversalElementRepresentationFromValue(valueValue);

		}
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
		int fromIndex = pageNumber * DebugUtils.PAGE_SIZE;
		final int mapSize = mapElements.size();
		int toIndex = (mapSize >= fromIndex + DebugUtils.PAGE_SIZE) ? (fromIndex + DebugUtils.PAGE_SIZE - 1)
				: mapSize - 1;
		InnerElementRepresentationDTO mapRepresentationDto = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
				.fromElement(mapRepresentation);
		MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> page = MapPageDTO
				.<InnerElementRepresentationDTO, InnerElementRepresentationDTO>builder().anchorMap(mapRepresentationDto)
				.elementName(mapRepresentation.getElementName()).elementType(mapRepresentation.getAdditionalInfo())
				.totalEntries(totalEntries).currentPage(pageNumber).totalPages(totalPages).fromIndex(fromIndex)
				.toIndex(toIndex).entries(collectionElements).anchorTag(mapRepresentation.getTag()).build();
		return page;
	}

//	private UniversalElementRepresentation createUniversalElementRepresentationFromValue(Value value) {
//		UniversalElementRepresentation element = null;
//		if (value instanceof ObjectReference objRef) {
//			String type = objRef.referenceType().name();
//			String valueText = type.startsWith("java.lang.") ? objRef.toString() : type;
//			element = UniversalElementRepresentation.builder().referenceType(objRef.referenceType())
//					.objectReference(objRef).elementName(valueText)
//					.elementType(UniversalElementRepresentation.UniversalElementType.COLLECTION_ELEMENT)
//					.currentRole(UniversalElementRepresentation.CurrentRole.INNER)
//					.value(DebugUtils.getObjectReferenceValueAsString(objRef))
//					.valueCategory(DebugUtils.determineValueCategory(value)).build();
//		}
//		return element;
//	}

	private NavigableMap<Integer, Entry<Value, Value>> waitForPageLoading() {
		if (Objects.isNull(pageNumber))
			pageNumber = 0;
		NavigableMap<Integer, Entry<Value, Value>> selectedItems = Collections.emptyNavigableMap();

		while (true) {
			selectedItems = mapElements.subMap(pageNumber * DebugUtils.PAGE_SIZE, true,
					pageNumber * DebugUtils.PAGE_SIZE + DebugUtils.PAGE_SIZE, false);
			if (selectedItems.size() == DebugUtils.PAGE_SIZE)
				return selectedItems;

			if (allElementsLoaded.get())
				return selectedItems;
			try {
				Thread.sleep(100);
			} catch (InterruptedException ignored) {
			}
		}
	}

	public List<UniversalElementRepresentation> populateSubordinatesElements(ObjectReference objRef) {

		if (objRef == null)
			return Collections.emptyList();
		List<UniversalElementRepresentation> subordinates = new ArrayList<>();
		DataProvider.jdiAccessLock.lock();
		try {
			ReferenceType refType = objRef.referenceType();
			for (Method method : refType.allMethods()) {

				if (DebugUtils.shouldSkipMethod(method))
					continue;
				List<String> args = method.argumentTypeNames();
				String methodArgs = String.join(", ", args);
				subordinates.add(
						UniversalElementRepresentation.builder()
								.referenceType(refType)
								.objectReference(objRef)
								.elementName(method.name() + "()")
								.additionalInfo(method.returnTypeName())
								.elementType(UniversalElementRepresentation.UniversalElementType.METHOD)
								.currentRole(UniversalElementRepresentation.CurrentRole.INNER)
								.value(refType.name() + "." + method.name() + "(" + methodArgs + ")")
								.isStatic(method.isStatic())
								.valueCategory(UniversalElementRepresentation.ValueCategory.NOT_SPECIFIED)
								.typeOrReturnType(method.returnTypeName())
								.uniqueId(UUID.randomUUID())
								.parentUniqueId(null)
								.level(-1)
								.build()
				);
			}
			for (Field field : refType.allFields()) {
				Value value = getFieldValue(field, objRef);
				String valueText = DebugUtils.getLocalVariableValueAsString(value);
				subordinates.add(
						UniversalElementRepresentation.builder()
								.referenceType(refType)
								.objectReference(objRef)
								.elementName(field.name())
								.additionalInfo(field.typeName())
								.elementType(UniversalElementRepresentation.UniversalElementType.FIELD)
								.currentRole(UniversalElementRepresentation.CurrentRole.INNER)
								.value(valueText)
								.isStatic(field.isStatic())
								.valueCategory(DebugUtils.determineValueCategory(value))
								.typeOrReturnType(field.typeName())
								.uniqueId(UUID.randomUUID())
								.parentUniqueId(null)
								.level(-1)
								.build()
				);
			}
		} finally {
			DataProvider.jdiAccessLock.unlock();
		}
		return subordinates;
	}

	private Value getFieldValue(Field field, ObjectReference instance) {
		try {
			if (field.isStatic()) {
				return field.declaringType().getValue(field);
			}
			if (instance != null) {
				return instance.getValue(field);
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

}