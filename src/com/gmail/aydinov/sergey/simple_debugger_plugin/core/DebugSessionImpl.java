package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationBreakpointRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeance;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserInvokedMethodEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceInnerElementInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceInspectionDTO;
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
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;

/**
 * Implementation of a debug session that processes JDI events, handles
 * breakpoints, updates UI, and allows variable/field modification and method
 * invocation in the target application.
 * <p>
 * Author: Sergei Aidinov <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class DebugSessionImpl implements DebugSession {

	public static final AtomicReference<String> methodInvocationResult = new AtomicReference<>("");
	private final EventSet eventSet;
	private final CurrentLineHighlighterImpl currentLineHighlighter;
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector simpleDebugEventCollector = SimpleDebuggerEventCollector.instance();
	private boolean shouldRefreshSnapsotAndUi = true;

	public DebugSessionImpl(EventSet eventSet) {
		this.eventSet = eventSet;
		this.currentLineHighlighter = new CurrentLineHighlighterImpl();
	}

	@Override
	public void run() {
		try {
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
			SimpleDebuggerLogger.info("DEBUG SESSION STARTED");
			simpleDebugEventCollector
					.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
			processEvents();
		} catch (Throwable exception) {
			logError("Fatal error in JDI event loop", exception);
		} finally {
			SimpleDebuggerLogger.info("DEBUG SESSION FINISHED");
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_FINISHED);
		}
	}

	/** Processes all events in the EventSet */
	private void processEvents() {
		TargetApplicationBreakpointRepresentation.getInstance().refreshBreakpoints();
		for (Event event : eventSet) {
			if (!DebuggerContext.context().isRunning())
				return;

			if (event instanceof BreakpointEvent breakpointEvent) {
				if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
					handleVmDisconnected();
					return;
				}

				doWorkAtBreakpoint(breakpointEvent);
				simpleDebugEventCollector.collectDebugEvent(
						new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
			}
		}
	}

	private void handleVmDisconnected() {
		try {
			TargetApplicationRepresentation.getInstance().detachDebugger();
			eventSet.resume();
		} catch (Exception ignored) {
		}
	}

	private void doWorkAtBreakpoint(BreakpointEvent breakpointEvent) {
		TargetApplicationRepresentation.getInstance().takeSnapshotOfTargetApplication(
				TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(), breakpointEvent);
		updateUI(breakpointEvent);
		Display display = Display.getDefault();
		if (Objects.nonNull(display) && !display.isDisposed()) {
			while (DebuggerContext.context().isDebugSessionActive()) {
				AbstractUIEvent uiEvent = uiEventCollector.pollUiEvent();
				if (Objects.isNull(uiEvent))
					continue;
				try {
					shouldRefreshSnapsotAndUi = true;
					handleSingleUiEvent(uiEvent, breakpointEvent);
					if (shouldRefreshSnapsotAndUi) {
						TargetApplicationRepresentation.getInstance().takeSnapshotOfTargetApplication(
								TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(), breakpointEvent);
						updateUI(breakpointEvent);
					}
				} catch (Throwable exception) {
					logError("Breakpoint handler error", exception);
				}

			}
		}
	}

	@SuppressWarnings("unchecked")
	private void handleSingleUiEvent(AbstractUIEvent abstractSimpleDebuggerUIEvent, BreakpointEvent breakpointEvent) {
		StackFrame currentFrame = getTopFrame(breakpointEvent.thread());
		if (Objects.isNull(currentFrame))
			return;
		try {
			if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_CHANGED_VARIABLE)) {
				shouldRefreshSnapsotAndUi = SimpleDebuggerEventType.USER_CHANGED_VARIABLE.getUiEventHandler()
						.handle(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_CHANGED_FIELD)) {
				shouldRefreshSnapsotAndUi = SimpleDebuggerEventType.USER_CHANGED_FIELD.getUiEventHandler()
						.handle(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_INVOKED_METHOD)) {
				// !!!!!!! UNDER CONSTRUCTION !!!!!!!
				shouldRefreshSnapsotAndUi = SimpleDebuggerEventType.USER_INVOKED_METHOD.getUiEventHandler()
						.handle(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_PRESSED_RESUME_BUTTON)) {
				shouldRefreshSnapsotAndUi = SimpleDebuggerEventType.USER_PRESSED_RESUME_BUTTON.getUiEventHandler()
						.handle(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_CLOSED_DEBUG_WINDOW)) {
				shouldRefreshSnapsotAndUi = SimpleDebuggerEventType.USER_CLOSED_DEBUG_WINDOW.getUiEventHandler()
						.handle(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_OBJECT)) {
				shouldRefreshSnapsotAndUi = SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_OBJECT
						.getUiEventHandler().handle(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_COLLECTION)) {
				shouldRefreshSnapsotAndUi = SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_COLLECTION
						.getUiEventHandler().handle(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
			} else {
				SimpleDebuggerLogger
						.info("Unhandled UI event: " + abstractSimpleDebuggerUIEvent.getClass().getSimpleName());
			}
		} catch (Exception exception) {
			SimpleDebuggerLogger.error(exception.getMessage(), exception);
		}
	}

	private void initiateInspectionSeanceIfPossible(InnerElementRepresentationDTO innerElementRepresentationDTO) {
		if (DebuggerContext.context().getStatus().equals(SimpleDebuggerStatus.INSPECTION_SEANCE_STARTING)
				|| DebuggerContext.context().isInspectionSeanceActive())
			return;
		Map<Tag, UniversalElementRepresentation> qq = TargetApplicationRepresentation.getInstance()
				.getTargetApplicationSnapshot();
		System.out.println(qq);
		TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().keySet().stream()
				.filter(v -> v.getUniqueId().equals(innerElementRepresentationDTO.getTag().getUniqueId())).findAny() // <-
																														// получаем
																														// Optional<UniversalElementRepresentation>
				.ifPresent(topLevelElementId -> {
					System.out.println("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq");
					try {
						UniversalElementRepresentation topLevelElement = TargetApplicationRepresentation.getInstance()
								.getTargetApplicationSnapshot().get(topLevelElementId);
						simpleDebugEventCollector.collectDebugEvent(new DebugEvent<Boolean>(
								SimpleDebuggerEventTypes.SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, false));
						simpleDebugEventCollector.collectDebugEvent(
								new DebugEvent<Boolean>(SimpleDebuggerEventType.DISPLAY_INSPECTION_WINDOW, true));

						InspectionSeance inspectionSession = new InspectionSeanceImpl(topLevelElement,
								TargetApplicationRepresentation.getInstance());
						// ...
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

		simpleDebugEventCollector.collectDebugEvent(new DebugEvent<Boolean>(
				SimpleDebuggerEventTypes.SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
		DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
	}

	private StackFrame getTopFrame(ThreadReference thread) {
		try {
			return thread.frame(0);
		} catch (Exception exception) {
			return null;
		}
	}

	private boolean updateUI(BreakpointEvent breakpointEvent) {
		if (Objects.isNull(breakpointEvent))
			return false;
		Location location = breakpointEvent.location();
		AtomicReference<ObjectReference> thisObjectRef = new AtomicReference<>();
		try {
			thisObjectRef.set(breakpointEvent.thread().frame(0).thisObject());
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
			return false;
		}
		ThreadReference thread = breakpointEvent.thread();
		StackFrame frame = null;
		try {
			frame = thread.frame(0);
		} catch (IncompatibleThreadStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (Objects.isNull(frame))
			return false;
		ObjectReference inctance = frame.thisObject();
		Optional<UniversalElementRepresentation> anchorInstanceOptional = TargetApplicationRepresentation.getInstance()
				.getTargetApplicationSnapshot().values().stream()
				.filter(e -> Objects.equals(e.getObjectReference(), inctance))
				.filter(e -> Objects.isNull(e.getTag().getParentId())).findAny();
		if (anchorInstanceOptional.isEmpty())
			return false;
		UniversalElementRepresentation anchorElement = anchorInstanceOptional.get();
		Set<UniversalElementRepresentation> relevantElements = selectFieldsAndMethods(anchorElement);
		List<UniversalElementRepresentation> locals = TargetApplicationRepresentation.getInstance()
				.getTargetApplicationSnapshot().values().stream()
				.filter(e -> e.getElementType().equals(UniversalElementType.LOCAL_VARIABLE)).toList();
		relevantElements.stream().forEach(e -> System.out.println("RL:" + e));
		relevantElements.addAll(locals);
		Set<InnerElementRepresentationDTO> innerElementDTOs = new HashSet();
		for (UniversalElementRepresentation element : relevantElements) {
			InnerElementRepresentationDTO elementRepresentation = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
					.fromUniversalElement(element);
			innerElementDTOs.add(elementRepresentation);
		}
		TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().values().stream()
				.forEach(e -> System.out.println("MD:" + e));
		System.out.println("INNER_ELS:" + innerElementDTOs);
		String methodName = location.declaringType().name() + "." + location.method().name() + "()";
		DebugWindowDataDTO debugWindowDataDTO = new DebugWindowDataDTO(location.lineNumber(), methodName,
				DebugUtils.compileStackInfo(thread), innerElementDTOs);
		simpleDebugEventCollector.collectDebugEvent(new DebugEvent<DebugWindowDataDTO>(
				SimpleDebuggerEventTypes.SimpleDebuggerEventType.STOPPED_AT_BREAKPOINT, debugWindowDataDTO));
		simpleDebugEventCollector
				.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
		currentLineHighlighter.highlight(breakpointEvent.location());
		System.out.println("relevantElements: " + relevantElements.size());
		return true;
	}

	private Set<UniversalElementRepresentation> selectFieldsAndMethods(UniversalElementRepresentation initElement) {
		if (Objects.isNull(initElement))
			return Collections.emptySet();
		Set<UniversalElementRepresentation> foundElements = new HashSet<>();
		foundElements.add(initElement);
		boolean found = true;
		while (found) {
			for (UniversalElementRepresentation earlierFoundElement : foundElements) {
				List<UniversalElementRepresentation> justFoundElements = new ArrayList<UniversalElementRepresentation>();
				justFoundElements.addAll(
						TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().values().stream()
								.filter(e -> Objects.equals(e.getObjectReference(), initElement.getObjectReference()))
								.filter(e -> Objects.equals(e.getTag().getParentId(),
										earlierFoundElement.getTag().getUniqueId()))
								.toList());
				if (justFoundElements.isEmpty()) {
					found = false;
					break;
				} else {
					foundElements.addAll(justFoundElements);
					justFoundElements.clear();
				}
			}
		}

		return foundElements;
	}

	private void logError(String message, Throwable exception) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, "simple_debugger_plugin", message, exception),
				StatusManager.LOG);
	}
}
