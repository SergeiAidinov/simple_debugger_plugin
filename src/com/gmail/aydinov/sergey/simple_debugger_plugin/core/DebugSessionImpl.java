package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugStoppedAtBreakepointEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.MethodInvokedEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.InvokeMethodEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserClosedWindowUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserPressedResumeUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.AbsentInformationException;
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
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;

public class DebugSessionImpl implements DebugSession {

	private final static AtomicReference<String> resultOfMethodInvocation = new AtomicReference<>("");
	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
	private final TargetApplicationRepresentation targetApplicationRepresentation;
	private final EventSet eventSet;
	private final CurrentLineHighlighter highlighter;

	private boolean debugSessionRunning = true;

	public DebugSessionImpl(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
			TargetApplicationRepresentation targetApplicationRepresentation, EventSet eventSet,
			CurrentLineHighlighter highlighter) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
		this.targetApplicationRepresentation = targetApplicationRepresentation;
		this.eventSet = eventSet;
		this.highlighter = highlighter;
	}

	@Override
	public void run() {
		try {
			System.out.println("DEBUG SESSION STARTED: " + LocalDateTime.now());
			process();
		} catch (Throwable t) {
			logError("Fatal error in JDI event loop", t);
		} finally {
			System.out.println("DEBUG SESSION FINISHED: " + LocalDateTime.now());
		}
	}

	private void process() {
		for (Event event : eventSet) {
			if (!debugSessionRunning)
				return;

			if (event instanceof BreakpointEvent breakpointEvent) {

				if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
					handleVmDisconnected();
					return;
				}

				refreshUI(breakpointEvent);

				while (debugSessionRunning) {
					AbstractUIEvent uiEvent = null;
					try {
						uiEvent = SimpleDebuggerEventQueue.instance().pollUiEvent();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (Objects.isNull(uiEvent))
						continue;
					targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation()
							.refreshBreakpoints();
					handleBreakpoint(breakpointEvent, uiEvent);
					refreshUI(breakpointEvent);
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

	private void handleBreakpoint(BreakpointEvent breakpointEvent, AbstractUIEvent uiEvent) {
		Display display = Display.getDefault();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(() -> {
				try {
					ITextEditor editor = openEditorForLocation(breakpointEvent.location());
					if (editor != null) {
						int line = breakpointEvent.location().lineNumber() - 1;
						highlighter.highlight(editor, line);
					}
				} catch (Throwable t) {
					logError("Cannot highlight breakpoint location", t);
				}
			});
		}

		try {
			handleSingleUiEvent(uiEvent, breakpointEvent);
		} catch (Throwable t) {
			logError("Breakpoint handler error", t);
		}

		if (debugSessionRunning)
			refreshUI(breakpointEvent);
	}

	private void handleSingleUiEvent(AbstractUIEvent uiEvent, BreakpointEvent breakpointEvent) {
		StackFrame frame = null;
		try {
			frame = breakpointEvent.thread().frame(0);
		} catch (IncompatibleThreadStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (Objects.isNull(frame))
			return;
		try {
			if (uiEvent instanceof UserChangedVariableDTO dto) {
				updateVariables(dto, frame);
			} else if (uiEvent instanceof UserChangedFieldDTO dto) {
				updateField(dto, frame);
			} else if (uiEvent instanceof InvokeMethodEvent evt) {
				invokeMethod(evt, breakpointEvent);
			} else if (uiEvent instanceof UserPressedResumeUiEvent) {
				System.out.println("User pressed RESUME");
				debugSessionRunning = false;
				// eventSet.resume();
			} else if (uiEvent instanceof UserClosedWindowUiEvent) {
				System.out.println("User closed debug window → stopping debug session only");
				debugSessionRunning = false;
				DebuggerContext.context().setRunning(false);
				targetVirtualMachineRepresentation.getVirtualMachine().dispose();
			} else {
				// Необработанное событие
				// System.out.println("Unhandled UI event: " +
				// uiEvent.getClass().getSimpleName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateVariables(UserChangedVariableDTO dto, StackFrame frame) {
		try {
			LocalVariable localVariable = frame.visibleVariables().stream().filter(v -> v.name().equals(dto.getName()))
					.findFirst().orElse(null);
			if (localVariable == null)
				return;

			Value val = DebugUtils.createJdiValueFromString(targetVirtualMachineRepresentation.getVirtualMachine(),
					localVariable, dto.getNewValue().toString());
			frame.setValue(localVariable, val);
		} catch (AbsentInformationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateField(UserChangedFieldDTO dto, StackFrame frame) throws Exception {
		ReferenceType refType = frame.thisObject() != null ? frame.thisObject().referenceType()
				: frame.location().declaringType();
		Field field = refType.fieldByName(dto.getFieldName());
		if (field == null)
			return;

		Value val = DebugUtils.createJdiObjectFromString(targetVirtualMachineRepresentation.getVirtualMachine(),
				field.type(), dto.getNewValue(), frame.thread());

		if (Modifier.isStatic(field.modifiers()) && refType instanceof ClassType ct) {
			ct.setValue(field, val);
		} else if (frame.thisObject() != null) {
			frame.thisObject().setValue(field, val);
		}
	}

	private void invokeMethod(InvokeMethodEvent event, BreakpointEvent breakpointEvent) {
		try {
			List<Value> args = DebugUtils.parseArguments(targetVirtualMachineRepresentation.getVirtualMachine(), event);
			ReferenceType refType = targetApplicationRepresentation.findReferenceTypeForClass(event.getClazz());
			Method method = refType.methodsByName(event.getMethod().getMethodName()).get(0);
			ObjectReference instance = !method.isStatic()
					? targetApplicationRepresentation.createObjectInstance((ClassType) refType)
					: null;

			Value result = instance != null
					? instance.invokeMethod(targetVirtualMachineRepresentation.getVirtualMachine().allThreads().get(0),
							method, args, ObjectReference.INVOKE_SINGLE_THREADED)
					: ((ClassType) refType).invokeMethod(
							targetVirtualMachineRepresentation.getVirtualMachine().allThreads().get(0), method, args,
							ClassType.INVOKE_SINGLE_THREADED);

			resultOfMethodInvocation.set(String.valueOf(result));
			SimpleDebuggerEventQueue.instance().collectDebugEvent(
					new MethodInvokedEvent(SimpleDebuggerEventType.METHOD_INVOKE, resultOfMethodInvocation.get()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private StackFrame getTopFrame(ThreadReference thread) {
		try {
			return thread.frame(0);
		} catch (Exception e) {
			return null;
		}
	}

	private boolean refreshUI(BreakpointEvent breakpointEvent) {
		if (breakpointEvent == null)
			return false;
		StackFrame frame = getTopFrame(breakpointEvent.thread());
		if (frame == null)
			return false;

		Location location = breakpointEvent.location();

		DebugStoppedAtBreakepointEvent dto = new DebugStoppedAtBreakepointEvent.Builder()
				.type(SimpleDebuggerEventType.STOPPED_AT_BREAKEPOINT).className(location.declaringType().name())
				.methodName(location.method().name()).lineNumber(location.lineNumber())
				.fields(DebugUtils.mapFields(DebugUtils.compileFields(frame)))
				.locals(DebugUtils.mapLocals(DebugUtils.compileLocalVariables(frame)))
				.stackTrace(resultOfMethodInvocation.get())
				.targetApplicationElementRepresentationList(discardVoidMethods(targetApplicationRepresentation.getTargetApplicationElements()))
				.methodCallInStacks(DebugUtils.compileStackInfo(breakpointEvent.thread()))
				.resultOfMethodInvocation(resultOfMethodInvocation.get().toString()).build();

		SimpleDebuggerEventQueue.instance().collectDebugEvent(dto);
		Display display = Display.getDefault();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(() -> {
				try {
					ITextEditor editor = openEditorForLocation(breakpointEvent.location());
					if (editor != null) {
						int line = breakpointEvent.location().lineNumber() - 1;
						highlighter.highlight(editor, line);
					}
				} catch (Throwable t) {
					logError("Cannot highlight breakpoint location", t);
				}
			});
		}
		return true;
	}

	private List<TargetApplicationElementRepresentation> discardVoidMethods(Iterable<TargetApplicationElementRepresentation> targetApplicationElements) {
		List<TargetApplicationElementRepresentation> withoutVoidMethods = new ArrayList<TargetApplicationElementRepresentation>();
		for (TargetApplicationElementRepresentation targetApplicationElementRepresentation : targetApplicationRepresentation
				.getTargetApplicationElements()) {
			TargetApplicationElementRepresentation copy = targetApplicationElementRepresentation.clone();
			Set<TargetApplicationMethodDTO> unvoidMathods = targetApplicationElementRepresentation.getMethods().stream()
					.filter(m -> !"void".equals(m.getReturnType())).collect(Collectors.toSet());
			copy.setMethods(unvoidMathods);
			withoutVoidMethods.add(copy);
		}
		return withoutVoidMethods;
	}

	private ITextEditor openEditorForLocation(Location location) throws Exception {
		if (location == null)
			return null;
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
		IWorkbenchPage page = window.getActivePage();
		if (page == null)
			return null;

		IFile file = targetApplicationRepresentation.findIFileForLocation(location);
		if (file == null)
			throw new IllegalStateException("Cannot map location to IFile: " + location);

		IEditorPart part = IDE.openEditor(page, file, true);
		if (part instanceof ITextEditor editor) {
			return editor;
		}
		throw new IllegalStateException("Opened editor is not a text editor");
	}

	@Override
	public void stop() {
		DebuggerContext.context().setRunning(false);
	}

	private void logError(String message, Throwable t) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, "simple_debugger_plugin", message, t),
				StatusManager.LOG);
	}
}