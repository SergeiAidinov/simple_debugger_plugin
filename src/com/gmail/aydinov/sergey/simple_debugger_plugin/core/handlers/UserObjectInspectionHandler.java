package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.Objects;
import java.util.Optional;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class UserObjectInspectionHandler implements UIEventHandler {

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();

	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		System.out.println("USER OBJECT. INSP. STARTED");
		debugEventCollector
				.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, false));
		// DebuggerContext.context().setStatus(SimpleDebuggerStatus.USER_OBJECT_INSPECTION_SEANCE_RUNNING);
		UIEvent<InnerElementRepresentationDTO> uiEvent = null;
		try {
			uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
		} catch (ClassCastException castException) {

		}
		if (Objects.nonNull(uiEvent)) {
			InnerElementRepresentationDTO userObject = uiEvent.getPayload();
			Optional<UniversalElementRepresentation> qq = findUserObject(userObject);
			UserObjectPageDTO userObjectPageDTO = UserObjectPageDTO.builder().elementName(userObject.getElementName())
					.classType(userObject.getTypeOrReturnType()).anchorTag(userObject.getTag()).build();

			try {
				debugEventCollector.collectDebugEvent(new DebugEvent<>(
						SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_USER_OBJECT, userObjectPageDTO));
//			Thread collectionInspectionThread = new Thread(
//					new UserObjectInspectionSeance(uiEvent.getPayload(), breakpointEvent));
//			collectionInspectionThread.setDaemon(true);
//			try {
//				collectionInspectionThread.start();
//				try {
//					collectionInspectionThread.join();
//				} catch (InterruptedException e) {
//					return false;
//				}

			} finally {
//				DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
//				debugEventCollector.collectDebugEvent(
//						new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
//				SimpleDebugerWindowsManager.instance().getUniversalInspectorWindow().close();

			}
		}
		return true;
	}

	private Optional<UniversalElementRepresentation> findUserObject(InnerElementRepresentationDTO anchor) {
		return TargetApplicationRepresentation.getInstance().getAllElements().stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.nonNull(e.getObjectReference()))
				.filter(e -> Objects.equals(e.getObjectReference().toString(), anchor.getAdditionalInfo())).findFirst();
	}

	private class UserObjectInspectionSeance implements Runnable {

		private final InnerElementRepresentationDTO inspectableElement;
		private final BreakpointEvent breakpointEvent;

		public UserObjectInspectionSeance(InnerElementRepresentationDTO inspectableElement,
				BreakpointEvent breakpointEvent) {
			this.inspectableElement = inspectableElement;
			this.breakpointEvent = breakpointEvent;
		}

		@Override
		public void run() {
			if (inspectableElement == null || breakpointEvent == null) {
				return;
			}
			innerElementInspection();
		}

		private void innerElementInspection() {
			Optional<UniversalElementRepresentation> userObjectOptional = findUserObject(inspectableElement);
			if (userObjectOptional.isEmpty())
				return;
			UniversalElementRepresentation userObject = userObjectOptional.get();
			UserObjectInspectionDTO userObjectInspectionDTO = UserObjectInspectionDTO.builder().tag(userObject.getTag())
					.className(userObject.getElementName()).build();

			debugEventCollector.collectDebugEvent(new DebugEvent<>(
					SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_USER_OBJECT, userObjectInspectionDTO));

		}

		private Optional<UniversalElementRepresentation> findUserObject(InnerElementRepresentationDTO anchor) {
			return TargetApplicationRepresentation.getInstance().getAllElements().stream()
					.filter(e -> e instanceof UniversalElementRepresentation)
					.map(e -> (UniversalElementRepresentation) e)
					.filter(e -> Objects.equals(e.getTag(), anchor.getTag())).findFirst();
		}

	}

}
