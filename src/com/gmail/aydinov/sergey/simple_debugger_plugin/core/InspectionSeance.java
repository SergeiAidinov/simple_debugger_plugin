package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable.AbstractInspectableElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable.InspectableInstanceElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable.InspectableIterableElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable.InspectableMapElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.BreadcrumbItemDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class InspectionSeance {

	private AbstractInspectableElement anchorElement;
//	private final ObjectReference anchorElementObjectReference;
	private final StackFrame currentFrame;
	private final BreakpointEvent breakpointEvent;
//	private final AbstractUIEvent initialSimpleDebuggerUIEvent;
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();

//	private final Deque<AbstractInspectableElement> inspectableQueue = new LinkedList<>();
	private final EventSequence eventSequence = new EventSequence();
	private static boolean alreadyStarted = false;
	private boolean ancorElementHandled = false;

	private InspectionSeance(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		this.currentFrame = currentFrame;
		this.breakpointEvent = breakpointEvent;
		UIEvent<InnerElementRepresentationDTO> uiEvent = null;
		try {
			uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
		} catch (ClassCastException castException) {

		}
		if (Objects.isNull(uiEvent)) return;
		eventSequence.put(uiEvent);
		startInspectionProcedure();
	}

	@SuppressWarnings("unchecked")
	public static boolean startInspectionSeanceForAnchorElement(AbstractUIEvent abstractSimpleDebuggerUIEvent,
			StackFrame currentFrame, BreakpointEvent breakpointEvent) {
		if (alreadyStarted)
			return false;
		// DebuggerContext.context().setStatus(SimpleDebuggerStatus.INSPECTION_SEANCE_RUNNING);
//		UIEvent<InnerElementRepresentationDTO> uiEvent = null;
//
//		try {
//			uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
//		} catch (ClassCastException castException) {
//
//		}
//		final long id = uiEvent.getPayload().getObjectId();
//		 Optional<UniversalElementRepresentation> i = TargetApplicationRepresentation.getInstance().getAllElements().stream()
//		.filter(e -> e instanceof UniversalElementRepresentation)
//		.map(e -> (UniversalElementRepresentation) e)
//		.filter(e -> Objects.equals(e.getObjectReferenceId(), id)).findAny();

//		if (Objects.isNull(uiEvent))
//			return false;
//		if (!(uiEvent instanceof UIEvent<?> rawEvent)) {
//			throw new IllegalArgumentException("Invalid event type: " + uiEvent);
//		}

//		Object payload = rawEvent.getPayload();
//		if (!(payload instanceof InnerElementRepresentationDTO dto)) {
//			throw new IllegalArgumentException("Invalid payload: " + payload);
//		}
//		AbstractInspectableElement anchorElement = null;
//		if (dto.getValueCategory().equals(ValueCategory.COLLECTION) || dto.getValueCategory().equals(ValueCategory.MAP))
//			anchorElement = AbstractInspectableElement.factory().createInspectableElement(abstractSimpleDebuggerUIEvent,
//					currentFrame, breakpointEvent);
//		else if (dto.getValueCategory().equals(ValueCategory.USER_OBJECT))
//			anchorElement = AbstractInspectableElement.factory().createInspectableElement(abstractSimpleDebuggerUIEvent,
//					currentFrame, breakpointEvent);
//		if (Objects.isNull(anchorElement))
//			return false;

		new InspectionSeance(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
		return true;
	}

	private void startInspectionProcedure() {
		alreadyStarted = true;
		InspectionProcedure inspectionProcedure = new InspectionProcedure();
		Thread inspectionThread = new Thread(inspectionProcedure);
		try {
			inspectionThread.start();
			inspectionThread.join();
		} catch (InterruptedException e) {
			SimpleDebuggerLogger.error(e.getMessage(), e);
		} finally {
			// DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
			debugEventCollector
					.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
			alreadyStarted = false;
		}

	}

	private class InspectionProcedure implements Runnable {

		@Override
		public void run() {
			// try {
			inspectionProcedure();
//			} finally {
//				debugEventCollector.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
//				DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
//			}
		}

		@SuppressWarnings("unchecked")
		private boolean inspectionProcedure() {
			// DebuggerContext.context().setStatus(SimpleDebuggerStatus.INSPECTION_SEANCE_RUNNING);
			debugEventCollector
					.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, false));
//			if (anchorElement instanceof InspectableIterableElement inspectableCollection) {
//				inspectableQueue.offer(inspectableCollection);
//				ArrayPageDTO page = (ArrayPageDTO) inspectableCollection.inspectPage(inspectableCollection, 0);
//				List<BreadcrumbItemDTO> qq = buildBreadcrumbs();
//				page.setBreadcrumbs(qq);
//				debugEventCollector.collectDebugEvent(
//						new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_ITERABLE, page));
//			} else if (anchorElement instanceof InspectableMapElement inspectableMapElement) {
//				MapPageDTO<UniversalElementRepresentation, UniversalElementRepresentation> page = (MapPageDTO<UniversalElementRepresentation, UniversalElementRepresentation>) inspectableMapElement
//						.inspectPage(inspectableMapElement, 0);
//				inspectableQueue.offer(inspectableMapElement);
//				List<BreadcrumbItemDTO> qq = buildBreadcrumbs();
//				page.setBreadcrumbs(qq);
//				debugEventCollector.collectDebugEvent(
//						new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_MAP, page));
////				 UIEventHandler handler = SimpleDebuggerEventType.USER_CONTINUES_INSPECTION_SEANCE_FOR_USER_OBJECT.getUiEventHandler();
////				 handler.handle(uiEvent, currentFrame, breakpointEvent);
//			} else if (anchorElement instanceof InspectableInstanceElement inspectableInstanceElement) {
//				inspectableQueue.offer(inspectableInstanceElement);
//				UserObjectPageDTO page = inspectableInstanceElement.inspectPage(inspectableInstanceElement);
//				page.setBreadcrumbs(buildBreadcrumbs());
//				UIEventHandler handler = SimpleDebuggerEventType.USER_INSPECTS_USER_OBJECT.getUiEventHandler();
//				handler.handle(initialSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
////				 debugEventCollector.collectDebugEvent(
////							new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_USER_OBJECT, page));
//			}

			while (true) {
				AbstractUIEvent uiEvent = null;
				if (ancorElementHandled) {
					try {
						uiEvent = uiEventCollector.takeUiEvent();
						System.out.println("EVENT IN SEANCE: " + uiEvent);
					} catch (InterruptedException e) {
					}
				} else {
					uiEvent = eventSequence.getEventByOrder(0);
					ancorElementHandled = true;
				}
				if (Objects.isNull(uiEvent))
					continue;
				if (!SimpleDebuggerEventTypes.isInspectionEvent(uiEvent.getType()))
					ignoreEvent(uiEvent);

				if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE))
					break;
				if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE)) {
					UIEventHandler handler = uiEvent.getType().getUiEventHandler();
					handler.handle(uiEvent, currentFrame, breakpointEvent);
//					UIEvent<Integer> userRequestetPage = (UIEvent<Integer>) uiEvent;
//					Integer pageNumber = userRequestetPage.getPayload();
//					InspectableIterableElement ic = (InspectableIterableElement) anchorElement;
//					ArrayPageDTO page = (ArrayPageDTO) ic.inspectPage(ic, pageNumber);
//					List<PairDTO<Integer, String>> qq = buildBreadcrumbs();
//					page.setBreadcrumbs(qq);
//					debugEventCollector.collectDebugEvent(
//							new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_ITERABLE, page));
//				} else if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_OBJECT)){
//					System.out.println("INSPECTION: " + uiEvent);
//					UIEventHandler handler = uiEvent.getType().getUiEventHandler();
//					handler.handle(uiEvent, currentFrame, breakpointEvent);
				} else if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_INSPECTS_USER_OBJECT)) {
					UIEventHandler handler = uiEvent.getType().getUiEventHandler();
					handler.handle(uiEvent, currentFrame, breakpointEvent);
				} else if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE)) {
					UIEventHandler handler = uiEvent.getType().getUiEventHandler();
					handler.handle(uiEvent, currentFrame, breakpointEvent);
				}
			}
			return true;
		}

		private void ignoreEvent(AbstractUIEvent debugEvent) {
			SimpleDebuggerLogger.info("Intentionally ignored: " + debugEvent);
		}

		private List<PairDTO<Integer, String>> buildBreadcrumbs() {
			List<PairDTO<Integer, String>> result = new ArrayList<>();
			for (Entry<Integer, UIEvent<InnerElementRepresentationDTO>> entry : eventSequence.getSequence().entrySet()) {
				result.add(PairDTO.of(entry.getKey(), entry.getValue().getPayload().getElementName()));
			}
			return result;
		}

	}
}
