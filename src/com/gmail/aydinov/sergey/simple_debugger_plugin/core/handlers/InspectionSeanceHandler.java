package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.CollectionPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class InspectionSeanceHandler implements UIEventHandler {

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();

	@SuppressWarnings("unchecked")
	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		System.out.println("COLLECT. INSP. STARTED");
		debugEventCollector
				.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, false));
		DebuggerContext.context().setStatus(SimpleDebuggerStatus.COLLECTION_INSPECTION_SEANCE_RUNNING);
		UIEvent<InnerElementRepresentationDTO> uiEvent = null;
		try {
			uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
		} catch (ClassCastException castException) {

		}
		if (Objects.nonNull(uiEvent)) {
			Thread collectionInspectionThread = new Thread(
					new CollectionInspectionSeance(uiEvent.getPayload(), breakpointEvent));
			collectionInspectionThread.setDaemon(false);
			try {
				collectionInspectionThread.start();
				try {
					collectionInspectionThread.join();
				} catch (InterruptedException e) {
					return false;
				}

			} finally {
				DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
				debugEventCollector.collectDebugEvent(
						new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
				SimpleDebugerWindowsManager.instance().getUniversalInspectorWindow().close();

			}
		}
		return true;
	}

	private class CollectionInspectionSeance implements Runnable {

		private final InnerElementRepresentationDTO anchorElement;
		private final BreakpointEvent breakpointEvent;
		private final TreeMap<Integer, InnerElementRepresentationDTO> colectionElements = new TreeMap<>();

		public CollectionInspectionSeance(InnerElementRepresentationDTO anchorElement,
				BreakpointEvent breakpointEvent) {
			this.anchorElement = anchorElement;
			this.breakpointEvent = breakpointEvent;
		}

		@Override
		public void run() {
			collectionInspection();
		}

		@SuppressWarnings("unchecked")
		private void collectionInspection() {
			compileCollectionElements();
			CollectionPageDTO initPage = createPage(0);
			debugEventCollector.collectDebugEvent(
					new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_COLLECTION, initPage));

			while (true) {
				AbstractUIEvent uiEvent = null;
				try {
					uiEvent = uiEventCollector.takeUiEvent();
					System.out.println("EVENT IN SEANCE: " + uiEvent);
				} catch (InterruptedException e) {
				}
				if (!SimpleDebuggerEventTypes.isCollectionInspectionWindowEvent(uiEvent.getType()))
					ignoreEvent(uiEvent);
				else if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE_FOR_COLLECTION))
					break;
				else if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE)) {
					UIEvent<Integer> userRequestetPage = (UIEvent<Integer>) uiEvent;
					Integer pageNumber = userRequestetPage.getPayload();
					CollectionPageDTO page = createPage(pageNumber);
					debugEventCollector.collectDebugEvent(
							new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_COLLECTION, page));
				}
			}
		}

		private CollectionPageDTO createPage(int pageNumber) {
			String elementType = (Objects.nonNull(colectionElements.get(0))
					&& Objects.nonNull(colectionElements.get(0).getTypeOrReturnType()))
							? colectionElements.get(0).getTypeOrReturnType()
							: DebugUtils.N_A;
			return CollectionPageDTO.builder().collectionName(anchorElement.getElementName())
					.collectionType(anchorElement.getTypeOrReturnType()).elementType(elementType)
					.totalElements(colectionElements.size()).currentPage(pageNumber)
					.totalPages(colectionElements.size() / DebugUtils.PAGE_SIZE).currentPage(pageNumber)
					.fromIndex(pageNumber).toIndex(pageNumber + DebugUtils.PAGE_SIZE - 1).entries(getPage(pageNumber))
					.build();
		}

		private void compileCollectionElements() {
			// 1. Берем внутренние элементы коллекции
			List<UniversalElementRepresentation> instances = TargetApplicationRepresentation.getInstance()
					.getTargetApplicationSnapshot().values().stream()
					.filter(e -> Objects.equals(e.getTag().getParentId(), anchorElement.getTag().getUniqueId()))
					.toList();

			// 2. Получаем ObjectReference для всех элементов коллекции
			Set<ObjectReference> collectionElementRefs = instances.stream()
					.map(UniversalElementRepresentation::getObjectReference).filter(Objects::nonNull)
					.flatMap(obj -> DebugUtils.iterateThroughCollection(obj, breakpointEvent).stream())
					.filter(ObjectReference.class::isInstance).map(ObjectReference.class::cast)
					.collect(Collectors.toSet());

			// 3. Связываем ObjectReference с реальным Comparable значением
			List<PairDTO<ObjectReference, Comparable<Object>>> sortedPairs = collectionElementRefs.stream().map(ref -> {
				Object value = DebugUtils.getComparableValue(ref);
				return PairDTO.<ObjectReference, Comparable<Object>>of(ref,
						value instanceof Comparable ? (Comparable<Object>) value : null);
			}).filter(p -> p.getSecond() != null).sorted((p1, p2) -> p1.getSecond().compareTo(p2.getSecond())).toList();

			// 4. Берем отсортированные ObjectReference
			List<ObjectReference> sortedRefs = sortedPairs.stream().map(PairDTO::getFirst).toList();

			// 5. Создаем DTO с уже установленным value
			List<InnerElementRepresentationDTO> readyRepresentationDTOs = sortedRefs.stream().map(ref -> {
				UniversalElementRepresentation uer = TargetApplicationRepresentation.getInstance()
						.getTargetApplicationSnapshot().values().stream()
						.filter(e -> ref.equals(e.getObjectReference())).findFirst().orElseThrow();
				InnerElementRepresentationDTO dto = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
						.fromUniversalElement(uer);
				dto.setValue(DebugUtils.getObjectReferenceValueAsString(ref));
				return dto;
			}).toList();

			for (int i = 0; i < readyRepresentationDTOs.size(); i++) {
				colectionElements.put(i, readyRepresentationDTOs.get(i));
			}

		}

		private List<PairDTO<Integer, InnerElementRepresentationDTO>> getPage(int pageNumber) {
			List<PairDTO<Integer, InnerElementRepresentationDTO>> result = new ArrayList<PairDTO<Integer, InnerElementRepresentationDTO>>();
			List<InnerElementRepresentationDTO> entries = List.copyOf(colectionElements
					.subMap((DebugUtils.PAGE_SIZE * pageNumber), true,
							(DebugUtils.PAGE_SIZE * pageNumber + DebugUtils.PAGE_SIZE), false)
					.values().stream().toList());
			for (int i = 0; i < entries.size(); i++) {
				result.add(PairDTO.of((i + DebugUtils.PAGE_SIZE * pageNumber), entries.get(i)));
			}
			return result;
		}

		private void ignoreEvent(AbstractUIEvent debugEvent) {
			SimpleDebuggerLogger.info("Intentionally ignored: " + debugEvent);
		}

	}
}
