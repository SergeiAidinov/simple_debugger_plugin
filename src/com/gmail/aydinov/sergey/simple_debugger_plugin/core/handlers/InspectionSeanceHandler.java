package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.SimpleDebugerWindowsManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ObjectReference;
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
			Thread collectionInspectionThread = new Thread(new CollectionInspectionSeance(uiEvent.getPayload(), breakpointEvent));
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
				debugEventCollector
						.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
				SimpleDebugerWindowsManager.instance().getUniversalInspectorWindow().close();
				
			}
		}
		return true;
	}

	private class CollectionInspectionSeance implements Runnable {
		
		private final InnerElementRepresentationDTO anchorElement;
		private final BreakpointEvent breakpointEvent;
		
		public CollectionInspectionSeance(InnerElementRepresentationDTO anchorElement, BreakpointEvent breakpointEvent) {
			this.anchorElement = anchorElement;
			this.breakpointEvent = breakpointEvent;
		}
		List<InnerElementRepresentationDTO> immediateElements = new ArrayList<InnerElementRepresentationDTO>();

		@Override
		public void run() {
			collectionInspection();
		}

		private void collectionInspection() {
		 List<UniversalElementRepresentation> qq = TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().values()
			.stream().filter(e -> Objects.equals( e.getTag().getParentId(), anchorElement.getTag().getUniqueId()))
			//.map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromUniversalElement(e))
			.toList();
		List<ObjectReference> yy = qq.stream().map(e -> DebugUtils.getCollectionElements(e.getObjectReference()))
				 .flatMap(List::stream)
				.toList();
		Set<ObjectReference> collectionElementRefs = qq.stream()
		        .map(UniversalElementRepresentation::getObjectReference)
		        .filter(Objects::nonNull)
		        .flatMap(obj -> DebugUtils.iterateThroughCollection(obj, breakpointEvent).stream())
		        .filter(ObjectReference.class::isInstance)
		        .map(ObjectReference.class::cast)
		        .collect(Collectors.toSet());

		List<UniversalElementRepresentation> matched = TargetApplicationRepresentation.getInstance()
		        .getTargetApplicationSnapshot()
		        .values()
		        .stream()
		        .filter(e -> collectionElementRefs.contains(e.getObjectReference()))
		        .toList();
	List<InnerElementRepresentationDTO> selected = matched.stream().map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromUniversalElement(e)).toList();
		
		
		List<InnerElementRepresentationDTO> tt = qq.stream().map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromUniversalElement(e)).toList();
		 
		  List<InnerElementRepresentationDTO> ww = qq.stream()
			       // .map(e -> DebugUtils.iterateThroughCollection(e.getObjectReference(), breakpointEvent))
				  .map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromUniversalElement(e))
			       // .flatMap(List::stream)
			        .toList();
		 System.out.println(ww);
		 
		// List<String> rr = ww.stream().map(e -> DebugUtils.valueToString(e)).toList();
		
			SimpleDebugerWindowsManager.instance().getUniversalInspectorWindow().showInspectableElement(PairDTO.of(anchorElement, selected));
			
			while (true) {
				AbstractUIEvent uiEvent = null;
				try {
					uiEvent = uiEventCollector.takeUiEvent();
					System.out.println("EVENT IN SEANCE: " + uiEvent);
				} catch (InterruptedException e) {}
				if (!SimpleDebuggerEventTypes.isCollectionInspectionWindowEvent(uiEvent.getType()))
					ignoreEvent(uiEvent);
				if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE_FOR_COLLECTION)) {
					break;
				}
					
				

			}

		}

		private void ignoreEvent(AbstractUIEvent debugEvent) {
			SimpleDebuggerLogger.info("Intentionally ignored: " + debugEvent);
		}

	}
}
