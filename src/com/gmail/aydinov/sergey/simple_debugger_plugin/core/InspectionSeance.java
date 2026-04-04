package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable.AbstractInspectableElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable.InspectableIterableElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable.InspectableMapElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionCollectionPage;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.BreadcrumbItemDTO;
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
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class InspectionSeance {

	private final AbstractInspectableElement anchorElement;
	private final StackFrame currentFrame;
	private final BreakpointEvent breakpointEvent;

	private final Deque<AbstractInspectableElement> inspectableQueue = new LinkedList<>();
	private static boolean alreadyStarted = false;

	private InspectionSeance(AbstractInspectableElement anchorElement, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		this.anchorElement = anchorElement;
		this.currentFrame = currentFrame;
		this.breakpointEvent = breakpointEvent;
	//	inspectableQueue.offer(anchorElement);
		startInspectionProcedure();
	}

	@SuppressWarnings("unchecked")
	public static boolean startInspectionSeanceForAnchorElement(AbstractUIEvent abstractSimpleDebuggerUIEvent,
			StackFrame currentFrame, BreakpointEvent breakpointEvent) {
		if (alreadyStarted)
			return false;
		UIEvent<InnerElementRepresentationDTO> uiEvent = null;
		try {
			uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
		} catch (ClassCastException castException) {

		}
		if (Objects.isNull(uiEvent))
			return false;
		AbstractInspectableElement anchorElement = AbstractInspectableElement.factory()
				.createInspectableElement(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
		if (Objects.isNull(anchorElement))
			return false;
		new InspectionSeance(anchorElement, currentFrame, breakpointEvent);
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
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
			alreadyStarted = false;
		}

	}

	private class InspectionProcedure implements Runnable {

		private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
		private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();

		@Override
		public void run() {
			inspectionProcedure();
		}

		@SuppressWarnings("unchecked")
		private boolean inspectionProcedure() {
			if (anchorElement instanceof InspectableIterableElement inspectableCollection) {
				InspectableIterableElement ic = (InspectableIterableElement) anchorElement;
				inspectableQueue.offer(ic);
				ArrayPageDTO page = (ArrayPageDTO) ic.inspectPage(ic, 0);
				List<BreadcrumbItemDTO> qq = buildBreadcrumbs();
				page.setBreadcrumbs(qq);
				debugEventCollector.collectDebugEvent(
						new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_ITERABLE, page));
			} else if (anchorElement instanceof InspectableMapElement inspectableMapElement) {
				 MapPageDTO<UniversalElementRepresentation, UniversalElementRepresentation> page = (MapPageDTO<UniversalElementRepresentation, UniversalElementRepresentation>) inspectableMapElement.inspectPage(inspectableMapElement, 0);
				 List<BreadcrumbItemDTO> qq = buildBreadcrumbs();
					page.setBreadcrumbs(qq);
					debugEventCollector.collectDebugEvent(
							new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_MAP, page));
			}

			while (true) {
				AbstractUIEvent uiEvent = null;
				try {
					uiEvent = uiEventCollector.takeUiEvent();
					System.out.println("EVENT IN SEANCE: " + uiEvent);
				} catch (InterruptedException e) {
				}
				if (Objects.isNull(uiEvent))
					continue;
				if (!SimpleDebuggerEventTypes.isInspectionEvent(uiEvent.getType()))
					ignoreEvent(uiEvent);

				if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE))
					break;
				if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_REQUESTED_COLLECTION_PAGE)) {
					UIEvent<Integer> userRequestetPage = (UIEvent<Integer>) uiEvent;
					Integer pageNumber = userRequestetPage.getPayload();
					InspectableIterableElement ic = (InspectableIterableElement) anchorElement;
					ArrayPageDTO page = (ArrayPageDTO) ic.inspectPage(ic, pageNumber);
					List<BreadcrumbItemDTO> qq = buildBreadcrumbs();
					page.setBreadcrumbs(qq);
					debugEventCollector.collectDebugEvent(
							new DebugEvent<>(SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_ITERABLE, page));
				}
			}
			return true;
		}

		private void ignoreEvent(AbstractUIEvent debugEvent) {
			SimpleDebuggerLogger.info("Intentionally ignored: " + debugEvent);
		}
		
		private List<BreadcrumbItemDTO> buildBreadcrumbs() {
		    List<BreadcrumbItemDTO> result = new ArrayList<>();

		    for (AbstractInspectableElement element : inspectableQueue) {
		        if (element == null) continue;

		        UniversalElementType type = element.getElementType();
		        boolean canInspect = element.isInspectable(); // или true, если метода нет
		        result.add(new BreadcrumbItemDTO(
		            element.getElementName(),
		            type,
		            element.getValueCategory(),
		            canInspect,
		            element.getTag()
		        ));
		    }

		    return result;
		}
		
		
	}
}
