package com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.TerminableDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ClassType;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

public class IterableDataProvider implements TerminableDataProvider {

	private final UniversalElementRepresentation iterableRepresentation;
	private final InspectionHandlerContext inspectionHandlerContext;

	private final NavigableMap<Integer, Value> elements = new ConcurrentSkipListMap<>();
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();

	private final AtomicInteger order = new AtomicInteger(0);
	private final AtomicBoolean allElementsLoaded = new AtomicBoolean(false);
	private final AtomicBoolean readingStarted = new AtomicBoolean(false);
	private final AtomicBoolean currentRequestActive = new AtomicBoolean(true);

	private ThreadReference thread;
	private ObjectReference iterator;

	private Method hasNextMethod;
	private Method nextMethod;
	private ClassType iteratorType;

	private Integer pageNumber;
	private volatile String totalPages = "calculating...";
	private volatile String totalEntries = "calculating...";

	private enum InitializationState {
		NOT_STARTED, IN_PROGRESS, SUCCESS, FAILED
	}

	private InitializationState initState = InitializationState.NOT_STARTED;

	public IterableDataProvider(UniversalElementRepresentation iterableRepresentation,
			InspectionHandlerContext inspectionHandlerContext) {
		super();
		this.iterableRepresentation = iterableRepresentation;
		this.inspectionHandlerContext = inspectionHandlerContext;
	}

	@Override
	public void handlePageRequest(Integer pageNumber) {
		this.pageNumber = (pageNumber == null) ? 0 : pageNumber;

		DataProvider.jdiAccessLock.lock();
		try {
			if (initState == InitializationState.NOT_STARTED) {
				initState = InitializationState.IN_PROGRESS;
				initState = initiate() ? InitializationState.SUCCESS : InitializationState.FAILED;
			}
		} finally {
			DataProvider.jdiAccessLock.unlock();
		}

		DataProvider.jdiAccessLock.lock();
		try {
			if (readingStarted.compareAndSet(false, true)) {
				iterateThroughIterable();
			}
		} finally {
			DataProvider.jdiAccessLock.unlock();
		}

		currentRequestActive.set(true);

		NavigableMap<Integer, Value> selectedItems = waitForPageLoading();

		ArrayPageDTO page = createPage(selectedItems);

		debugEventCollector.collectDebugEvent(new DebugEvent<>(
				SimpleDebuggerEventTypes.SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_ITERABLE, page));

		debugEventCollector.collectDebugEvent(
				new DebugEvent<>(SimpleDebuggerEventTypes.SimpleDebuggerEventType.CLOSE_LOADING_POPUP, null));
	}

	private ArrayPageDTO createPage(NavigableMap<Integer, Value> selectedItems) {

		Map<Integer, UniversalElementRepresentation> entries = new LinkedHashMap<>();
		// Integer index = 0;
		for (Entry<Integer, Value> entry : selectedItems.entrySet()) {

			// Integer index = outer.getKey();
			Value value = entry.getValue();

			if (value instanceof ObjectReference objectReference) {

				UniversalElementRepresentation representation = UniversalElementRepresentation.builder()
						.objectReference(objectReference).referenceType(objectReference.referenceType())
						.elementName(DebugUtils.getObjectReferenceValueAsString(objectReference))
						.elementType(UniversalElementRepresentation.UniversalElementType.COLLECTION_ELEMENT)
						.valueCategory(DebugUtils.determineValueCategory(value))
						.value(DebugUtils.getObjectReferenceValueAsString(objectReference)).uniqueId(UUID.randomUUID())
						.build();

				entries.put(entry.getKey(), representation);
			} else {

				UniversalElementRepresentation representation = UniversalElementRepresentation.builder()
						.objectReference(null).referenceType(null).elementName(String.valueOf(value))
						.elementType(UniversalElementRepresentation.UniversalElementType.COLLECTION_ELEMENT)
						.valueCategory(DebugUtils.determineValueCategory(value)).value(String.valueOf(value))
						.uniqueId(UUID.randomUUID()).build();

				entries.put(entry.getKey(), representation);
			}
		}

		int fromIndex = pageNumber * DebugUtils.PAGE_SIZE;
		int toIndex = Math.min(elements.size(), fromIndex + DebugUtils.PAGE_SIZE);

		return ArrayPageDTO.builder().objectId(iterableRepresentation.getObjectReferenceId())
				.elementName(iterableRepresentation.getElementName())
				.elementType(iterableRepresentation.getAdditionalInfo()).totalElements(totalEntries)
				.currentPage(pageNumber).totalPages(totalPages).fromIndex(fromIndex).toIndex(toIndex).entries(entries)
				.anchorTag(iterableRepresentation.getTag()).build();
	}

	private void iterateThroughIterable() {
		new Thread(() -> {
			while (true) {
				Value hasNextVal;

				DataProvider.jdiAccessLock.lock();
				try {
					hasNextVal = iterator.invokeMethod(thread, hasNextMethod, Collections.emptyList(),
							ObjectReference.INVOKE_SINGLE_THREADED);

					if (!(hasNextVal instanceof BooleanValue bv) || !bv.value()) {
						break;
					}

					Value next = iterator.invokeMethod(thread, nextMethod, Collections.emptyList(),
							ObjectReference.INVOKE_SINGLE_THREADED);

					if (next instanceof ObjectReference ref) {
						inspectionHandlerContext.getInspectionSeanceCache().getLoadedPieces().put(ref.uniqueID(), ref);
					}

					int index = order.getAndIncrement();
					elements.put(index, next);

				} catch (Exception ignored) {
				} finally {
					DataProvider.jdiAccessLock.unlock();
				}
			}

			allElementsLoaded.set(true);
			totalEntries = String.valueOf(elements.size());

			int total = Integer.parseInt(totalEntries);
			int pages = (total + DebugUtils.PAGE_SIZE - 1) / DebugUtils.PAGE_SIZE;
			totalPages = String.valueOf(pages);

		}).start();
	}

	private boolean initiate() {
		ReferenceType refType = iterableRepresentation.getObjectReference().referenceType();
		if (!(refType instanceof ClassType classType)) {
			return false;
		}

		boolean isIterable = classType.allInterfaces().stream().anyMatch(i -> "java.lang.Iterable".equals(i.name()));

		if (!isIterable) {
			return false;
		}

		thread = inspectionHandlerContext.getBreakpointEvent().thread();

		Method iteratorMethod = classType.concreteMethodByName("iterator", "()Ljava/util/Iterator;");
		if (iteratorMethod == null) {
			return false;
		}

		Value iteratorValue;

		try {
			iteratorValue = iterableRepresentation.getObjectReference().invokeMethod(thread, iteratorMethod,
					Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
		} catch (Exception e) {
			return false;
		}

		if (!(iteratorValue instanceof ObjectReference it)) {
			return false;
		}

		this.iterator = it;

		iteratorType = (ClassType) iterator.referenceType();
		hasNextMethod = iteratorType.concreteMethodByName("hasNext", "()Z");
		nextMethod = iteratorType.concreteMethodByName("next", "()Ljava/lang/Object;");

		return hasNextMethod != null && nextMethod != null;
	}

	private NavigableMap<Integer, Value> waitForPageLoading() {
		if (pageNumber == null) {
			pageNumber = 0;
		}

		NavigableMap<Integer, Value> selected;

		while (currentRequestActive.get()) {
			selected = elements.subMap(pageNumber * DebugUtils.PAGE_SIZE, true,
					pageNumber * DebugUtils.PAGE_SIZE + DebugUtils.PAGE_SIZE, false);

			if (selected.size() == DebugUtils.PAGE_SIZE || allElementsLoaded.get()) {
				return selected;
			}

			try {
				Thread.sleep(200);
			} catch (InterruptedException ignored) {
			}
		}

		return Collections.emptyNavigableMap();
	}

	@Override
	public void terminateCurrentRequest() {
		// TODO Auto-generated method stub

	}

}
