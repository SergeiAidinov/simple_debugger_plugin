package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class InspectionSeance {

	private final StackFrame currentFrame;
	private final BreakpointEvent breakpointEvent;
	private final AbstractUIEvent initialSimpleDebuggerUIEvent;
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();
	private boolean initialEventHandled = false;
	private final AtomicInteger breadCrumbOrder = new AtomicInteger(0);
	private static final AtomicBoolean alreadyStarted = new AtomicBoolean(false);
	public static AbstractInspectionDTO lastInspectedAbstractInspectionDTO = null;
	public final static Map<Long, DataProviderHolder> inspectionSeanceCache = new ConcurrentHashMap<Long, DataProviderHolder>();
	public final static SortedMap<Integer, BreadCrumb> breadcrumbs = new java.util.concurrent.ConcurrentSkipListMap<>();

	private InspectionSeance(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		this.currentFrame = currentFrame;
		this.breakpointEvent = breakpointEvent;
		this.initialSimpleDebuggerUIEvent = abstractSimpleDebuggerUIEvent;
		if (Objects.isNull(initialSimpleDebuggerUIEvent))
			return;
		DebuggerContext.context().defineInspectionSeanceId();
		startInspectionProcedure();
	}

	public static List<PairDTO<Integer, String>> getBreadCrumbs() {
		List<PairDTO<Integer, String>> result = new ArrayList<PairDTO<Integer, String>>();
		for (BreadCrumb breadCrumb : breadcrumbs.values()) {
			result.add(PairDTO.of(breadCrumb.getBreadCrumbOrder(), breadCrumb.getDescription()));
		}
		return result;
	}

	public static boolean startInspectionSeanceForAnchorElement(AbstractUIEvent abstractSimpleDebuggerUIEvent,
			StackFrame currentFrame, BreakpointEvent breakpointEvent) {
		if (!alreadyStarted.compareAndSet(false, true))
			return false;

		new InspectionSeance(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
		return true;
	}

	private void startInspectionProcedure() {
		InspectionProcedure inspectionProcedure = new InspectionProcedure();
		Thread inspectionThread = new Thread(inspectionProcedure);
		try {
			inspectionThread.start();
			inspectionThread.join();
		} catch (InterruptedException e) {
			SimpleDebuggerLogger.error(e.getMessage(), e);
		} finally {
			debugEventCollector
					.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
			for (DataProviderHolder dataProviderHolder : inspectionSeanceCache.values()) {
				dataProviderHolder.getThread().interrupt();
			}
			inspectionSeanceCache.clear();
			breadcrumbs.clear();
			alreadyStarted.set(false);
		}

	}

	private class InspectionProcedure implements Runnable {

		@Override
		public void run() {
			inspectionProcedure();
		}

		@SuppressWarnings("unchecked")
		private boolean inspectionProcedure() {
			debugEventCollector
					.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, false));
			while (true) {
				AbstractUIEvent uiEvent = null;
				if (initialEventHandled) {
					try {
						uiEvent = uiEventCollector.takeUiEvent();
						System.out.println("EVENT IN SEANCE: " + uiEvent);
					} catch (InterruptedException e) {
					}
				} else {
					uiEvent = initialSimpleDebuggerUIEvent;
					initialEventHandled = true;
				}
				if (Objects.isNull(uiEvent))
					continue;
				if (!SimpleDebuggerEventTypes.isInspectionEvent(uiEvent.getType())) {
					ignoreEvent(uiEvent);
					continue;
				}
				if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE))
					break;
				if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE)) {
					UIEventHandler handler = uiEvent.getType().getUiEventHandler();
					handler.handle(uiEvent, currentFrame, breakpointEvent);
				} else if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_INSPECTS_USER_OBJECT)) {
					UIEvent<InnerElementRepresentationDTO> userInspectsUserObjectEvent = (UIEvent<InnerElementRepresentationDTO>) uiEvent;
					if (lastInspectedAbstractInspectionDTO instanceof MapPageDTO mapPageDTO) {
//						List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> l = mapPageDTO
//								.getEntries();
//						System.out.println(l);
//						Optional<InnerElementRepresentationDTO> w = l.stream().map(e -> e.getSecond()).filter(e -> e instanceof InnerElementRepresentationDTO)
//								.map(e -> (InnerElementRepresentationDTO) e)
//								.filter(e -> Objects.equals(e.getObjectId(), userInspectsUserObjectEvent.getPayload().getObjectId())).findAny();
//						System.out.println(w);
//						w.get().getObjectId();
					}
					
					UIEventHandler handler = uiEvent.getType().getUiEventHandler();
					handler.handle(uiEvent, currentFrame, breakpointEvent);
				} else if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_REQUESTED_MAP_PAGE)) {
					UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>> userReqeustedMapPageEvent = (UIEvent<PairDTO<InnerElementRepresentationDTO, Integer>>) uiEvent;
					String descriprion = userReqeustedMapPageEvent.getPayload().getFirst().getElementName() + " page: "
							+ userReqeustedMapPageEvent.getPayload().getSecond();
					addBreadCrumbIfNecessary(userReqeustedMapPageEvent.getPayload().getFirst().getObjectId(),
							userReqeustedMapPageEvent, descriprion);
					UIEventHandler handler = uiEvent.getType().getUiEventHandler();
					handler.handle(userReqeustedMapPageEvent, currentFrame, breakpointEvent);
				}
			}
			return true;
		}

		private void addBreadCrumbIfNecessary(Long objectId, AbstractUIEvent abstractUIEvent, String description) {
			final int order = breadCrumbOrder.getAndIncrement();
			BreadCrumb breadCrumb = new BreadCrumb(order, objectId, abstractUIEvent, description);
			breadcrumbs.put(order, breadCrumb);

		}

		private void ignoreEvent(AbstractUIEvent debugEvent) {
			SimpleDebuggerLogger.info("Intentionally ignored: " + debugEvent);
		}
	}
}
