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
			collectionInspectionThread.setDaemon(true);
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
			collectionInspection(anchorElement);
		}

		@SuppressWarnings("unchecked")
		private void collectionInspection(InnerElementRepresentationDTO anchorElement) {
			compileCollectionElements(anchorElement);
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

		private void compileCollectionElements(InnerElementRepresentationDTO anchorElement) {
			// 1. Берем внутренние элементы коллекции
			List<AbstractElementRepresentation> allElements = new ArrayList<AbstractElementRepresentation>(
					TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().getFirst().values());
			allElements.addAll(
					TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().getSecond().values());
			Optional<UniversalElementRepresentation> collectionElementOptional = allElements.stream()
					.filter(e -> e instanceof UniversalElementRepresentation)
					.map(e -> (UniversalElementRepresentation) e)
					.filter(e -> Objects.equals(e.getTag(), anchorElement.getTag())).findAny();
			if (collectionElementOptional.isEmpty())
				return;
			UniversalElementRepresentation collectionElement = collectionElementOptional.get();
			Map<AbstractElementRepresentation.Tag, AbstractElementRepresentation> collectionElements = new HashMap<AbstractElementRepresentation.Tag, AbstractElementRepresentation>();
			List<Value> qq = DebugUtils.iterateThroughCollection(collectionElement.getObjectReference(),
					breakpointEvent);
			if (qq.isEmpty()) {
				Optional<UniversalElementRepresentation> ww = allElements.stream()
						.filter(e -> e instanceof UniversalElementRepresentation)
						.map(e -> (UniversalElementRepresentation) e)
						.filter(e -> Objects.equals(e.getElementType(), UniversalElementType.LOCAL_VARIABLE))
						.filter(e -> Objects.equals(e.getElementName(), anchorElement.getElementName())).findAny();
				System.out.println(ww);
				ObjectReference collectionRef = ww.get().getObjectReference();
				ReferenceType rr = ww.get().getReferenceType();
				List<Value> items = DebugUtils.iterateThroughCollection(collectionRef, breakpointEvent);
				System.out.println(items);
			}
			List<UniversalElementRepresentation> result = new ArrayList();
			for (Value v : qq) {
				if (v == null)
					continue;
				// 🔹 1. Если это объект
				if (v instanceof ObjectReference objRef) {
					long id = objRef.uniqueID();
					String type = objRef.referenceType().name();
					System.out.println("OBJ -> id=" + id + ", type=" + type);
					UniversalElementRepresentation uer = collectionElements.values().stream()
							.filter(e -> e instanceof UniversalElementRepresentation)
							.map(e -> (UniversalElementRepresentation) e).filter(e -> {
								ObjectReference r = e.getObjectReference();
								return r != null && r.uniqueID() == objRef.uniqueID();
							}).findFirst().orElseGet(() -> {
								String valueText = type.startsWith("java.lang.") ? objRef.toString() : type;
								return UniversalElementRepresentation.builder().referenceType(objRef.referenceType())
										.objectReference(objRef).elementName(valueText) // 👈 без индекса
										.additionalInfo(type).elementType(UniversalElementType.COLLECTION_ELEMENT)
										.currentRole(CurrentRole.INNER)
										.value(DebugUtils.getObjectReferenceValueAsString(objRef))
										.valueCategory(DebugUtils.determineValueCategory(v)).uniqueId(UUID.randomUUID())
										.parentUniqueId(collectionElement.getTag().getUniqueId())
										.level(collectionElement.getLevel() + 1).build();
							});

					result.add(uer);
				}

				// 🔹 2. Примитивы
				else {
					System.out.println("PRIMITIVE -> " + v);

					UniversalElementRepresentation primitiveElement = UniversalElementRepresentation.builder()
							.elementName("item").value(v.toString()).valueCategory(ValueCategory.PRIMITIVE)
							.uniqueId(UUID.randomUUID()).parentUniqueId(collectionElement.getTag().getUniqueId())
							.level(collectionElement.getLevel() + 1).build();

					result.add(primitiveElement);
				}
			}
			System.out.println(result);
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
