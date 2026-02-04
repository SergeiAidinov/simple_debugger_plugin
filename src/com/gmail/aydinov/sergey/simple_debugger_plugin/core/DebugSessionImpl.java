package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugStoppedAtBreakpointEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.MethodInvokedEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.InvokeMethodEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserChangedFieldEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserChangedVariableEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserClosedWindowUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserPressedResumeUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
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
 */
public class DebugSessionImpl implements DebugSession {

	private static final AtomicReference<String> methodInvocationResult = new AtomicReference<>("");
	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
	private final TargetApplicationRepresentation targetApplicationRepresentation;
	private final EventSet eventSet;
	private final CurrentLineHighlighter currentLineHighlighter;

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
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.SESSION_STARTED);
			SimpleDebuggerLogger.info("DEBUG SESSION STARTED");
			processEvents();
		} catch (Throwable exception) {
			logError("Fatal error in JDI event loop", exception);
		} finally {
			SimpleDebuggerLogger.info("DEBUG SESSION FINISHED");
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

				updateUI(breakpointEvent);

				while (DebuggerContext.context().isSessionActive()) {
					AbstractUIEvent uiEvent = SimpleDebuggerEventQueue.instance().pollUiEvent();
					if (Objects.isNull(uiEvent))
						continue;
					targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation()
							.refreshBreakpoints();
					handleBreakpointEvent(breakpointEvent, uiEvent);
					if (DebuggerContext.context().isRunning()) {
						updateUI(breakpointEvent);
					}
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

	private void handleBreakpointEvent(BreakpointEvent breakpointEvent, AbstractUIEvent uiEvent) {
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
			handleSingleUiEvent(uiEvent, breakpointEvent);
		} catch (Throwable exception) {
			logError("Breakpoint handler error", exception);
		}

		if (DebuggerContext.context().isRunning()) {
			updateUI(breakpointEvent);
		}
	}

	private void handleSingleUiEvent(AbstractUIEvent uiEvent, BreakpointEvent breakpointEvent) {
		StackFrame currentFrame = getTopFrame(breakpointEvent.thread());
		if (Objects.isNull(currentFrame))
			return;

		try {
			if (uiEvent instanceof UserChangedVariableEvent variableEvent) {
				updateLocalVariable(variableEvent, currentFrame);
			} else if (uiEvent instanceof UserChangedFieldEvent fieldEvent) {
				updateField(fieldEvent, currentFrame);
			} else if (uiEvent instanceof InvokeMethodEvent invokeEvent) {
				invokeMethod(invokeEvent, breakpointEvent, currentFrame);
			} else if (uiEvent instanceof UserPressedResumeUiEvent) {
				SimpleDebuggerLogger.info("User pressed RESUME");
				DebuggerContext.context().setStatus(SimpleDebuggerStatus.SESSION_FINISHED);
			} else if (uiEvent instanceof UserClosedWindowUiEvent) {
				SimpleDebuggerLogger.info("User closed debug window → stopping debug session");
				DebuggerContext.context().setStatus(SimpleDebuggerStatus.STOPPED);
				targetVirtualMachineRepresentation.getVirtualMachine().dispose();
			} else {
				SimpleDebuggerLogger.info("Unhandled UI event: " + uiEvent.getClass().getSimpleName());
			}
		} catch (Exception exception) {
			SimpleDebuggerLogger.error(exception.getMessage(), exception);
		}
	}

	private void updateLocalVariable(UserChangedVariableEvent variableEvent, StackFrame currentFrame) {
		try {
			LocalVariable localVariable = currentFrame.visibleVariables().stream()
					.filter(v -> v.name().equals(variableEvent.getName())).findFirst().orElse(null);

			if (Objects.isNull(localVariable))
				return;

			Value value = DebugUtils.createJdiValueFromString(targetVirtualMachineRepresentation.getVirtualMachine(),
					localVariable, variableEvent.getNewValue().toString());
			currentFrame.setValue(localVariable, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateField(UserChangedFieldEvent fieldEvent, StackFrame currentFrame) throws Exception {
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

	private void invokeMethod(InvokeMethodEvent invokeEvent, BreakpointEvent breakpointEvent, StackFrame currentFrame) {
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
			SimpleDebuggerEventQueue.instance().collectDebugEvent(
					new MethodInvokedEvent(SimpleDebuggerEventType.METHOD_INVOKE, methodInvocationResult.get()));
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

		DebugStoppedAtBreakpointEvent debugEvent = new DebugStoppedAtBreakpointEvent.Builder()
				.type(SimpleDebuggerEventType.STOPPED_AT_BREAKPOINT).className(location.declaringType().name())
				.methodName(location.method().name()).lineNumber(location.lineNumber())
				.fields(DebugUtils.mapFields(DebugUtils.compileFields(currentFrame)))
				.locals(DebugUtils.mapLocals(DebugUtils.compileLocalVariables(currentFrame)))
				.stackTrace(methodInvocationResult.get())
				.targetApplicationElements(
						discardVoidMethods(targetApplicationRepresentation.getTargetApplicationElements()))
				.methodCallInStacks(DebugUtils.compileStackInfo(breakpointEvent.thread()))
				.resultOfMethodInvocation(methodInvocationResult.get()).build();

		SimpleDebuggerEventQueue.instance().collectDebugEvent(debugEvent);

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

	private List<TargetApplicationElementRepresentation> discardVoidMethods(
			Iterable<TargetApplicationElementRepresentation> targetElements) {
		List<TargetApplicationElementRepresentation> withoutVoidMethods = new ArrayList<>();
		for (TargetApplicationElementRepresentation element : targetApplicationRepresentation
				.getTargetApplicationElements()) {
			TargetApplicationElementRepresentation copy = element.clone();
			Set<TargetApplicationMethodDTO> nonVoidMethods = element.getMethods().stream()
					.filter(m -> !"void".equals(m.getReturnType())).collect(Collectors.toSet());
			copy.setMethods(nonVoidMethods);
			withoutVoidMethods.add(copy);
		}
		return withoutVoidMethods;
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
