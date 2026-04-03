package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.CollectionPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
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
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class IterableInspectionSeanceHandler implements UIEventHandler {

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();

	@SuppressWarnings("unchecked")
	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
//		System.out.println("COLLECT. INSP. STARTED");
//		debugEventCollector
//				.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, false));
//		DebuggerContext.context().setStatus(SimpleDebuggerStatus.INSPECTION_SEANCE_RUNNING);
//		UIEvent<InnerElementRepresentationDTO> uiEvent = null;
//		try {
//			uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
//			if (Objects.nonNull(uiEvent)) {
//				CollectionInspectionSeance collectionInspectionSeance = new CollectionInspectionSeance(
//						uiEvent.getPayload(), breakpointEvent);
//				collectionInspectionSeance.collectionInspection(uiEvent.getPayload());
//			}
//		} catch (ClassCastException castException) {
//
//		} finally {
//			DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
//			debugEventCollector
//					.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
//			SimpleDebugerWindowsManager.instance().getUniversalInspectorWindow().close();
//		}
		return true;
	}

	private class CollectionInspectionSeance {
//
//		private final InnerElementRepresentationDTO anchorElement;
//		private final BreakpointEvent breakpointEvent;
//		private String elementType = "";
//		private String collectionType = "";
//		private final TreeMap<Integer, InnerElementRepresentationDTO> collectionElements = new TreeMap<>();
//
//		public CollectionInspectionSeance(InnerElementRepresentationDTO anchorElement,
//				BreakpointEvent breakpointEvent) {
//			this.anchorElement = anchorElement;
//			this.breakpointEvent = breakpointEvent;
//		}
//
//		@SuppressWarnings("unchecked")
//		public void collectionInspection(InnerElementRepresentationDTO anchorElement) {
//			compileCollectionElements(anchorElement);
//			// ww = DebugUtils.determinCollectionType(anchorElement, breakpointEvent);
//			CollectionPageDTO initPage = createPage(0);
//			debugEventCollector.collectDebugEvent(
//					new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_ITERABLE, initPage));
//
//			while (true) {
//				AbstractUIEvent uiEvent = null;
//				try {
//					uiEvent = uiEventCollector.takeUiEvent();
//					System.out.println("EVENT IN SEANCE: " + uiEvent);
//				} catch (InterruptedException e) {
//				}
//				if (!SimpleDebuggerEventTypes.isCollectionInspectionWindowEvent(uiEvent.getType()))
//					ignoreEvent(uiEvent);
//				else if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE))
//					break;
//				else if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE)) {
//					UIEvent<Integer> userRequestetPage = (UIEvent<Integer>) uiEvent;
//					Integer pageNumber = userRequestetPage.getPayload();
//					CollectionPageDTO page = createPage(pageNumber);
//					debugEventCollector.collectDebugEvent(
//							new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_ITERABLE, page));
//				}
//			}
//		}
//
//		private CollectionPageDTO createPage(int pageNumber) {
//			return CollectionPageDTO.builder().collectionName(anchorElement.getElementName())
//					.collectionType(collectionType).elementType(elementType).totalElements(collectionElements.size())
//					.currentPage(pageNumber).totalPages((collectionElements.size() / DebugUtils.PAGE_SIZE) + 1)
//					.fromIndex(pageNumber * DebugUtils.PAGE_SIZE)
//					.toIndex(pageNumber * DebugUtils.PAGE_SIZE + DebugUtils.PAGE_SIZE - 1).entries(getPage(pageNumber))
//					.anchorTag(anchorElement.getTag()).build();
//		}
//
//		private void compileCollectionElements(InnerElementRepresentationDTO anchorElement) {
//			// Получаем объект коллекции по тегу
//			Optional<UniversalElementRepresentation> collectionOpt = TargetApplicationRepresentation.getInstance()
//					.getAllElements() // предполагаем метод, который объединяет first и
//					.stream().filter(e -> e instanceof UniversalElementRepresentation)
//					.map(e -> (UniversalElementRepresentation) e)
//					.filter(e -> Objects.equals(e.getTag(), anchorElement.getTag())).findAny();
//
//			if (collectionOpt.isEmpty())
//				return;
//
//			UniversalElementRepresentation collection = collectionOpt.get();
//			TripletDTO<String, String, String> ww = DebugUtils.determinCollectionType(collection.getObjectReference(),
//					breakpointEvent);
//			collectionType = ww.getFirst();
//			elementType = ww.getSecond();
//			// Получаем все значения коллекции
//			List<Value> values = DebugUtils.iterateThroughCollection(collection.getObjectReference(), breakpointEvent);
//
//			// Преобразуем в InnerElementRepresentationDTO и кладём в colectionElements
//			for (int i = 0; i < values.size(); i++) {
//				Value v = values.get(i);
//				if (v == null)
//					continue;
//
//				UniversalElementRepresentation uer;
//				if (v instanceof ObjectReference objRef) {
//					String type = objRef.referenceType().name();
//					String valueText = type.startsWith("java.lang.") ? objRef.toString() : type;
//
//					uer = UniversalElementRepresentation.builder().referenceType(objRef.referenceType())
//							.objectReference(objRef).elementName(valueText)
//							.elementType(UniversalElementType.COLLECTION_ELEMENT).currentRole(CurrentRole.INNER)
//							.value(DebugUtils.getObjectReferenceValueAsString(objRef))
//							.valueCategory(DebugUtils.determineValueCategory(v))
//							// .typeOrReturnType(DebugUtils.)
//							.uniqueId(UUID.randomUUID()).parentUniqueId(collection.getTag().getUniqueId())
//							.level(collection.getLevel() + 1).build();
//				} else {
//					// Примитив
//					uer = UniversalElementRepresentation.builder().elementName("item").value(v.toString())
//							.valueCategory(ValueCategory.PRIMITIVE).uniqueId(UUID.randomUUID())
//							.parentUniqueId(collection.getTag().getUniqueId()).level(collection.getLevel() + 1).build();
//				}
//
//				InnerElementRepresentationDTO dto = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
//						.fromElement(uer);
//				collectionElements.put(i, dto);
//			}
//		}
//
//		private List<PairDTO<Integer, InnerElementRepresentationDTO>> getPage(int pageNumber) {
//			List<PairDTO<Integer, InnerElementRepresentationDTO>> result = new ArrayList<PairDTO<Integer, InnerElementRepresentationDTO>>();
//			List<InnerElementRepresentationDTO> entries = List.copyOf(collectionElements
//					.subMap((DebugUtils.PAGE_SIZE * pageNumber), true,
//							(DebugUtils.PAGE_SIZE * pageNumber + DebugUtils.PAGE_SIZE), false)
//					.values().stream().toList());
//			for (int i = 0; i < entries.size(); i++) {
//				result.add(PairDTO.of((i + DebugUtils.PAGE_SIZE * pageNumber), entries.get(i)));
//			}
//			return result;
//		}
//
//		private void ignoreEvent(AbstractUIEvent debugEvent) {
//			SimpleDebuggerLogger.info("Intentionally ignored: " + debugEvent);
//		}
	}
}
