package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.ITextEditor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TopLevelElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserInvokedMethodEventDTO;
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
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
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

	private static final AtomicReference<String> methodInvocationResult = new AtomicReference<>("");
	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
	private final TargetApplicationRepresentation targetApplicationRepresentation;
	private final EventSet eventSet;
	private final CurrentLineHighlighter currentLineHighlighter;
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector simpleDebugEventCollector = SimpleDebuggerEventCollector.instance();

	public DebugSessionImpl(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
			TargetApplicationRepresentation targetApplicationRepresentation, EventSet eventSet,
			CurrentLineHighlighter currentLineHighlighter) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
		this.targetApplicationRepresentation = targetApplicationRepresentation;
		this.eventSet = eventSet;
		this.currentLineHighlighter = currentLineHighlighter;
	}

	@Override
	public void run() {
		try {
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
			SimpleDebuggerLogger.info("DEBUG SESSION STARTED");
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
		for (Event event : eventSet) {
			if (!DebuggerContext.context().isRunning())
				return;

			if (event instanceof BreakpointEvent breakpointEvent) {
				if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
					handleVmDisconnected();
					return;
				}
				targetApplicationRepresentation.addLocalVaraibles(targetVirtualMachineRepresentation.getVirtualMachine(), breakpointEvent);
				updateUI(breakpointEvent);

				while (DebuggerContext.context().isDebugSessionActive()) {
					AbstractUIEvent uiEvent = uiEventCollector.pollUiEvent();
					if (Objects.isNull(uiEvent))
						continue;
					targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation()
							.refreshBreakpoints();
					doWorkAtBreakpoint(breakpointEvent, uiEvent);
				}
			}
		}
	}

	private void handleVmDisconnected() {
		try {
			targetApplicationRepresentation.detachDebugger();
			eventSet.resume();
		} catch (Exception ignored) {
		}
	}

	private void doWorkAtBreakpoint(BreakpointEvent breakpointEvent, AbstractUIEvent uiEvent) {
		Display display = Display.getDefault();
		if (Objects.nonNull(display) && !display.isDisposed()) {
			display.asyncExec(() -> {
				try {
					ITextEditor editor = openEditorForLocation(breakpointEvent.location());
					if (Objects.nonNull(editor)) {
						int lineNumber = breakpointEvent.location().lineNumber() - 1;
						currentLineHighlighter.highlight(editor, lineNumber);
					}
				} catch (Throwable exception) {
					logError("Cannot highlight breakpoint location", exception);
				}
			});
		}

		try {
			targetApplicationRepresentation.takeSnapshotOfTargetApplication(
					targetVirtualMachineRepresentation.getVirtualMachine());
			handleSingleUiEvent(uiEvent, breakpointEvent);
			targetApplicationRepresentation.addLocalVaraibles(targetVirtualMachineRepresentation.getVirtualMachine(), breakpointEvent);
		} catch (Throwable exception) {
			logError("Breakpoint handler error", exception);
		}

		if (DebuggerContext.context().isRunning()) {
			updateUI(breakpointEvent);
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
				UIEvent<UserChangedVariableEventDTO> userChangedVariableEvent = (UIEvent<UserChangedVariableEventDTO>) abstractSimpleDebuggerUIEvent;
				updateLocalVariable(userChangedVariableEvent.getPayload(), currentFrame);
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_CHANGED_FIELD)) {
				UIEvent<UserChangedFieldEventDTO> userChangedFieldEvent = (UIEvent<UserChangedFieldEventDTO>) abstractSimpleDebuggerUIEvent;
				updateField(userChangedFieldEvent.getPayload(), currentFrame);
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_INVOKED_METHOD)) {
				UIEvent<UserInvokedMethodEventDTO> userInvokedMethodEvent = (UIEvent<UserInvokedMethodEventDTO>) abstractSimpleDebuggerUIEvent;
				invokeMethod(userInvokedMethodEvent.getPayload(), breakpointEvent, currentFrame);
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_PRESSED_RESUME_BUTTON)) {
				SimpleDebuggerLogger.info("User pressed RESUME");
				DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_FINISHED);
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_CLOSED_DEBUG_WINDOW)) {
				SimpleDebuggerLogger.info("User closed debug window → stopping debug session");
				DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUGGER_STOPPED);
				targetVirtualMachineRepresentation.getVirtualMachine().dispose();
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_STARTED_INSPECTION_SESSION_FOR_ELEMENT)) {
				UIEvent<InnerElementRepresentationDTO> userStartedInspectionSeanceEvent = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
				initiateInspectionSeanceIfPossible(userStartedInspectionSeanceEvent.getPayload());
			} else {
				SimpleDebuggerLogger
						.info("Unhandled UI event: " + abstractSimpleDebuggerUIEvent.getClass().getSimpleName());
			}
		} catch (Exception exception) {
			SimpleDebuggerLogger.error(exception.getMessage(), exception);
		} finally {
			targetApplicationRepresentation.takeSnapshotOfTargetApplication(
					targetVirtualMachineRepresentation.getVirtualMachine());
		}
	}

	private void initiateInspectionSeanceIfPossible(InnerElementRepresentationDTO innerElementRepresentationDTO) {
//		if (DebuggerContext.context().getStatus().equals(SimpleDebuggerStatus.INSPECTION_SEANCE_STARTING)
//				|| DebuggerContext.context().isInspectionSeanceActive())
//			return;
//		Optional.ofNullable(targetApplicationRepresentation.getTargetApplicationSnapshot().values().stream()
//				.filter(v -> v.getSecond().getUniqueId().equals(innerElementRepresentationDTO.getUniqueId())))
//				.ifPresent(topLevelElement -> {
//					try {
//						simpleDebugEventCollector.collectDebugEvent(new DebugEvent<Boolean>(
//								SimpleDebuggerEventTypes.SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, false));
//						simpleDebugEventCollector.collectDebugEvent(
//								new DebugEvent<Boolean>(SimpleDebuggerEventType.DISPLAY_INSPECTION_WINDOW, true));
//
//						InspectionSeance inspectionSession = new InspectionSeanceImpl(
//								(TopLevelElementRepresentation) topLevelElement, targetApplicationRepresentation);
//						Thread inspectionSessionThread = new Thread(inspectionSession);
//						inspectionSessionThread.setDaemon(true);
//						inspectionSessionThread.start();
//
//						inspectionSessionThread.join();
//					} catch (InterruptedException e) {
//						Thread.currentThread().interrupt();
//					} catch (Exception e) {
//						SimpleDebuggerLogger.error("Inspection error", e);
//					} finally {
//						simpleDebugEventCollector.collectDebugEvent(new DebugEvent<Boolean>(
//								SimpleDebuggerEventTypes.SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
//						DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
//					}
//				});

		simpleDebugEventCollector.collectDebugEvent(new DebugEvent<Boolean>(
				SimpleDebuggerEventTypes.SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
		DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
	}

	private void updateLocalVariable(UserChangedVariableEventDTO userChangedVariableEventDTO, StackFrame currentFrame) {
		try {
			LocalVariable localVariable = currentFrame.visibleVariables().stream()
					.filter(v -> v.name().equals(userChangedVariableEventDTO.getName())).findFirst().orElse(null);

			if (Objects.isNull(localVariable))
				return;

			Value value = DebugUtils.createJdiValueFromString(targetVirtualMachineRepresentation.getVirtualMachine(),
					localVariable, userChangedVariableEventDTO.getNewValue().toString());
			currentFrame.setValue(localVariable, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateField(UserChangedFieldEventDTO fieldEvent, StackFrame currentFrame) throws Exception {
		ReferenceType referenceType = Objects.nonNull(currentFrame.thisObject())
				? currentFrame.thisObject().referenceType()
				: currentFrame.location().declaringType();
		Field field = referenceType.fieldByName(fieldEvent.getFieldName());
		if (Objects.isNull(field))
			return;

		Value value = DebugUtils.createJdiObjectFromString(targetVirtualMachineRepresentation.getVirtualMachine(),
				field.type(), fieldEvent.getNewValue(), currentFrame.thread());

		if (Modifier.isStatic(field.modifiers()) && referenceType instanceof ClassType classType) {
			classType.setValue(field, value);
		} else if (Objects.nonNull(currentFrame.thisObject())) {
			currentFrame.thisObject().setValue(field, value);
		}
	}

	private void invokeMethod(UserInvokedMethodEventDTO invokeEvent, BreakpointEvent breakpointEvent,
			StackFrame currentFrame) {
		try {
			List<Value> methodArguments = DebugUtils
					.parseArguments(targetVirtualMachineRepresentation.getVirtualMachine(), invokeEvent);
			ReferenceType referenceType = targetApplicationRepresentation
					.findReferenceTypeForClass(invokeEvent.getTargetClass());
			Method method = referenceType.methodsByName(invokeEvent.getMethod().getMethodName()).get(0);
			ObjectReference instance = !method.isStatic()
					? targetApplicationRepresentation.createObjectInstance((ClassType) referenceType)
					: null;

			Value result = Objects.nonNull(instance)
					? instance.invokeMethod(targetVirtualMachineRepresentation.getVirtualMachine().allThreads().get(0),
							method, methodArguments, ObjectReference.INVOKE_SINGLE_THREADED)
					: ((ClassType) referenceType).invokeMethod(
							targetVirtualMachineRepresentation.getVirtualMachine().allThreads().get(0), method,
							methodArguments, ClassType.INVOKE_SINGLE_THREADED);

			methodInvocationResult.set(String.valueOf(result));
			simpleDebugEventCollector.collectDebugEvent(new DebugEvent<String>(
					SimpleDebuggerEventTypes.SimpleDebuggerEventType.METHOD_INVOKE, methodInvocationResult.get()));
		} catch (Exception exception) {
			exception.printStackTrace();
		}
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

		StackFrame currentFrame = getTopFrame(breakpointEvent.thread());
		if (Objects.isNull(currentFrame))
			return false;

		Location location = breakpointEvent.location();
		ReferenceType referenceType = location.declaringType();
		AtomicReference<UniversalElementRepresentation> anchorElementReference = new AtomicReference<UniversalElementRepresentation>();
		targetApplicationRepresentation.getTargetApplicationSnapshot().values().stream()
				.filter (v -> v.getReferenceType().equals(referenceType)).findAny()
				.ifPresent(v -> anchorElementReference.set(v));
		if (Objects.isNull(anchorElementReference.get()))
			return false;
		DebugWindowDataDTO debugWindowDataDTO = new DebugWindowDataDTO(anchorElementReference.get(), location);
		ThreadReference thread = breakpointEvent.thread();
		StackFrame frame = null;
		try {
			frame = thread.frame(0);
		} catch (IncompatibleThreadStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<LocalVariable, Value> locals = DebugUtils.compileLocalVariables(frame);
		
	

		List<InnerElementRepresentationDTO> localVariables = locals.entrySet().stream()
		        .map(entry -> {
		            LocalVariable var = entry.getKey();
		            Value value = entry.getValue();
		            return new InnerElementRepresentationDTO(
		                    UUID.randomUUID(),       // уникальный идентификатор
		                    debugWindowDataDTO.getUniqueId(),
		                    var.name(),              // имя переменной
		                    var.typeName(),          // полное имя типа
		                    UniversalElementType.VARIABLE, // тип элемента
		                    value != null ? value.toString() : "null", // значение
		                    debugWindowDataDTO.isStatic(),
		                    debugWindowDataDTO.getValueCategory(),
		                    value.type().name()
		            );
		        })
		        .toList();

		if (!localVariables.isEmpty()) {
			debugWindowDataDTO.getInnerElements().addAll(localVariables);
		}
		debugWindowDataDTO.setStackCall(DebugUtils.compileStackInfo(thread));
		simpleDebugEventCollector.collectDebugEvent(new DebugEvent<DebugWindowDataDTO>(
				SimpleDebuggerEventTypes.SimpleDebuggerEventType.STOPPED_AT_BREAKPOINT, debugWindowDataDTO));
		simpleDebugEventCollector
				.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));

		Display display = Display.getDefault();
		if (Objects.nonNull(display) && !display.isDisposed()) {
			display.asyncExec(() -> {
				try {
					ITextEditor editor = openEditorForLocation(breakpointEvent.location());
					if (Objects.nonNull(editor)) {
						int lineNumber = breakpointEvent.location().lineNumber() - 1;
						currentLineHighlighter.highlight(editor, lineNumber);
					}
				} catch (Throwable exception) {
					logError("Cannot highlight breakpoint location", exception);
				}
			});
		}

		return true;
	}

	private List<TopLevelElementRepresentationDTO> discardVoidMethods(List<TopLevelElementRepresentationDTO> targetElements) {
		return targetElements;
	}

	private ITextEditor openEditorForLocation(Location location) throws Exception {
		if (Objects.isNull(location))
			return null;

		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (Objects.isNull(workbenchWindow))
			return null;

		IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
		if (Objects.isNull(workbenchPage))
			return null;

		IFile file = targetApplicationRepresentation.findIFileForLocation(location);
		if (Objects.isNull(file))
			throw new IllegalStateException("Cannot map location to IFile: " + location);

		IEditorPart editorPart = IDE.openEditor(workbenchPage, file, true);
		if (editorPart instanceof ITextEditor textEditor) {
			return textEditor;
		}
		throw new IllegalStateException("Opened editor is not a text editor");
	}

	private void logError(String message, Throwable exception) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, "simple_debugger_plugin", message, exception),
				StatusManager.LOG);
	}
}
