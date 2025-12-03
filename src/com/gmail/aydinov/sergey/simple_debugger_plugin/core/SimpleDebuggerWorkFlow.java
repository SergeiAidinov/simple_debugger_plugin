package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.EventLoop;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.InvokeMethodEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserClosedWindowUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserPressedResumeUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.TypeComponent;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.request.EventRequestManager;

public class SimpleDebuggerWorkFlow {

    private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
    private final TargetApplicationRepresentation targetApplicationRepresentation;
    private final IBreakpointManager iBreakpointManager; // do NOT remove!!!
	private final BreakpointSubscriberRegistrar breakpointListener; // do NOT remove!!!
    private final EventLoop eventLoop;
    private final SimpleDebugEventCollector simpleDebugEventCollector = SimpleDebuggerEventQueue.instance();
    private volatile boolean running = true;
    private final AtomicReference<String> resultOfMethodInvocation = new AtomicReference<>("");

    public SimpleDebuggerWorkFlow(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
			IBreakpointManager iBreakpointManager, BreakpointSubscriberRegistrar breakpointListener) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
		this.iBreakpointManager = iBreakpointManager;
		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine()
				.eventRequestManager();
		this.breakpointListener = breakpointListener;
		this.targetApplicationRepresentation = new TargetApplicationRepresentation(iBreakpointManager,
				eventRequestManager, targetVirtualMachineRepresentation.getVirtualMachine(), breakpointListener);
		this.eventLoop = new JdiEventLoop(
		        targetVirtualMachineRepresentation.getVirtualMachine(), // VirtualMachine
		        this::onBreakpointEvent,                                 // BreakpointEventHandler
		        this::onVmStopped, // VmLifeCycleHandler
		        targetApplicationRepresentation 
		);

	}

    /** Запуск дебага */
    public void debug() {
    	refreshBreakpoints();
        openDebugWindow();
        targetApplicationRepresentation.refreshReferencesToClassesOfTargetApplication(targetVirtualMachineRepresentation.getVirtualMachine());
        eventLoop.runLoop();
       // detachDebugger();
    }

    /** Обработчик остановки VM */
    private void onVmStopped() {
        running = false;
        detachDebugger();
        Display.getDefault().asyncExec(() -> {
            DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
            if (window != null) window.showVmStoppedMessage();
        });
    }

    /** Обработчик брейкпоинта */
    private void onBreakpointEvent(BreakpointEvent event) {
        //highlightLine(event.location());
        targetApplicationRepresentation.refreshReferencesToClassesOfTargetApplication(targetVirtualMachineRepresentation.getVirtualMachine());
        refreshUI(event);

        StackFrame frame = getTopFrame(event.thread());
        handleUiLoop(event, frame);
    }

    /** UI-loop на брейкпоинте */
    private void handleUiLoop(BreakpointEvent event, StackFrame frame) {
        while (running) {
            UIEvent uiEvent;
            try {
                uiEvent = SimpleDebuggerEventQueue.instance().pollUiEvent(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (uiEvent == null) continue;

            if (uiEvent instanceof UserPressedResumeUiEvent) break;
            if (uiEvent instanceof UserClosedWindowUiEvent) { handleUserClosedWindow(); break; }

            handleSingleUiEvent(uiEvent, frame);
            targetApplicationRepresentation.refreshReferencesToClassesOfTargetApplication(targetVirtualMachineRepresentation.getVirtualMachine());
            refreshUI(event);
        }
    }
    
    private void refreshBreakpoints() {
		targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation().refreshBreakePoints();
	}

    private void handleSingleUiEvent(UIEvent event, StackFrame frame) {
        try {
            if (event instanceof UserChangedVariableDTO dto) updateVariables(dto, frame);
            if (event instanceof UserChangedFieldDTO dto) updateField(dto, frame);
            if (event instanceof InvokeMethodEvent evt) invokeMethod(evt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateVariables(UserChangedVariableDTO dto, StackFrame frame) {
        LocalVariable var = null;
		try {
			var = frame.visibleVariables().stream()
			        .filter(v -> v.name().equals(dto.getName()))
			        .findFirst().orElse(null);
		} catch (AbsentInformationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (var == null) return;
        try {
            Value val = DebugUtils.createJdiValueFromString(targetVirtualMachineRepresentation.getVirtualMachine(), var, dto.getNewValue().toString());
            frame.setValue(var, val);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateField(UserChangedFieldDTO dto, StackFrame frame) throws Exception {
        ReferenceType refType = frame.thisObject() != null ? frame.thisObject().referenceType() : frame.location().declaringType();
        Field field = refType.fieldByName(dto.getFieldName());
        if (field == null) return;

        Value val = DebugUtils.createJdiObjectFromString(targetVirtualMachineRepresentation.getVirtualMachine(), field.type(), dto.getNewValue(), frame.thread());

        if (Modifier.isStatic(field.modifiers()) && refType instanceof ClassType ct) {
            ct.setValue(field, val);
        } else if (frame.thisObject() != null) {
            frame.thisObject().setValue(field, val);
        }
    }

    private void invokeMethod(InvokeMethodEvent event) {
        try {
            List<Value> args = DebugUtils.parseArguments(targetVirtualMachineRepresentation.getVirtualMachine(), event);
            ReferenceType refType = targetApplicationRepresentation.findReferenceTypeForClass(event.getClazz());
            Method method = refType.methodsByName(event.getMethod().getMethodName()).get(0);
            ObjectReference instance = !method.isStatic() ? targetApplicationRepresentation.createObjectInstance((ClassType) refType) : null;
            Value result = instance != null
                    ? instance.invokeMethod(targetVirtualMachineRepresentation.getVirtualMachine().allThreads().get(0), method, args, ObjectReference.INVOKE_SINGLE_THREADED)
                    : ((ClassType) refType).invokeMethod(targetVirtualMachineRepresentation.getVirtualMachine().allThreads().get(0), method, args, ClassType.INVOKE_SINGLE_THREADED);

            resultOfMethodInvocation.set(String.valueOf(result));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void detachDebugger() {
        try { targetApplicationRepresentation.detachDebugger(); } catch (Exception ignored) {}
    }

    private void handleUserClosedWindow() { running = false; detachDebugger(); }

    private StackFrame getTopFrame(ThreadReference thread) {
        try { return thread.frame(0); } catch (Exception e) { return null; }
    }

    private void openDebugWindow() {
        Display.getDefault().asyncExec(() -> {
            DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
            if (window != null && !window.isOpen()) window.open();
        });
    }

//    private void highlightLine(Location loc) {
//        Display.getDefault().asyncExec(() -> {
//            try {
//                ITextEditor editor = openEditorForLocation(loc);
//                if (editor != null) new CurrentLineHighlighter().highlight(editor, loc.lineNumber() - 1);
//            } catch (Exception ignored) {}
//        });
//    }

    private boolean refreshUI(BreakpointEvent breakpointEvent) {
        if (breakpointEvent == null) return false;
        StackFrame frame = getTopFrame(breakpointEvent.thread());
        if (frame == null) return false;

        Map<LocalVariable, Value> locals = Map.of();
        try { locals = frame.getValues(frame.visibleVariables()); } catch (Exception ignored) {}
        Map<Field, Value> fields = Map.of();
        try { if (frame.thisObject() != null) fields = frame.thisObject().getValues(frame.thisObject().referenceType().fields()); } catch (Exception ignored) {}

         Location location = breakpointEvent.location();
         Map<LocalVariable, Value> localVariables = Collections.emptyMap();
		try {
			localVariables = frame.getValues(frame.visibleVariables()).entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		} catch (AbsentInformationException e) {
			System.err.println("No debug info: " + e.getMessage());
		} catch (com.sun.jdi.InvalidStackFrameException e) {
			System.err.println("Cannot read variables: " + e.getMessage());
			return false;
		}
					
		SimpleDebugEventDTO dto = new SimpleDebugEventDTO.Builder()
		        .type(SimpleDebugEventType.REFRESH_DATA)
		        .className(location .declaringType().name())
		        .methodName(location.method().name())
		        .lineNumber(location.lineNumber())
		        .fields(DebugUtils.mapFields(fields))
		        .locals(DebugUtils.mapLocals(localVariables))
		        .stackTrace( resultOfMethodInvocation.get())
		        .targetApplicationElementRepresentationList(targetApplicationRepresentation.getTargetApplicationElements())
		        .methodCallInStacks(DebugUtils.compileStackInfo(breakpointEvent.thread()))
		        .resultOfMethodInvocation(resultOfMethodInvocation.get().toString())
		        .build();

        simpleDebugEventCollector.collectDebugEvent(dto);
        return true;
    }

    private ITextEditor openEditorForLocation(Location location) throws Exception {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        IFile file = targetApplicationRepresentation.findIFileForLocation(location);
        IEditorPart part = IDE.openEditor(page, file, true);
        if (!(part instanceof ITextEditor editor)) throw new IllegalStateException("Not a text editor");
        return editor;
    }



    public static class Factory {

        private static SimpleDebuggerWorkFlow instance = null;
        private static SimpleDebuggerStatus simpleDebuggerStatus = SimpleDebuggerStatus.STARTING;

        public static SimpleDebuggerWorkFlow getSimpleDebuggerWorkFlow() {
            return instance;
        }

        public static SimpleDebuggerStatus getSimpleDebuggerStatus() {
            return simpleDebuggerStatus;
        }

        public static void create(String host, int port, OnWorkflowReadyListener listener) {
            CompletableFuture.runAsync(() -> {
                VirtualMachine vm = attachToVm(host, port);
                IBreakpointManager bpm = waitForBreakpointManager();

                BreakePointListener breakpointListener = new BreakePointListener();
                bpm.setEnabled(true);
                bpm.addBreakpointListener(breakpointListener);

                instance = new SimpleDebuggerWorkFlow(
                        new TargetVirtualMachineRepresentation(host, port, vm),
                        bpm,
                        breakpointListener
                );

                if (listener != null) {
                    Display.getDefault().asyncExec(() -> listener.onReady(instance));
                }
            });
        }

        // -------------------
        private static VirtualMachine attachToVm(String host, int port) {
            VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
            AttachingConnector connector = vmm.attachingConnectors().stream()
                    .filter(c -> c.name().equals("com.sun.jdi.SocketAttach"))
                    .findAny()
                    .orElseThrow();

            Map<String, Connector.Argument> args = connector.defaultArguments();
            args.get("hostname").setValue(host);
            args.get("port").setValue(String.valueOf(port));

            while (true) {
                try {
                    System.out.println("Connecting to " + host + ":" + port + "...");
                    VirtualMachine vm = connector.attach(args);
                    simpleDebuggerStatus = SimpleDebuggerStatus.VM_CONNECTED;
                    System.out.println("Successfully connected to VM.");
                    return vm;
                } catch (Exception ignored) {
                    simpleDebuggerStatus = SimpleDebuggerStatus.VM_AWAITING_CONNECTION;
                    try { Thread.sleep(1000); } catch (InterruptedException ignored2) {}
                }
            }
        }

        // -------------------
        private static IBreakpointManager waitForBreakpointManager() {
            CompletableFuture<IBreakpointManager> future = new CompletableFuture<>();

            Runnable check = new Runnable() {
                @Override
                public void run() {
                    DebugPlugin plugin = DebugPlugin.getDefault();
                    if (plugin != null && plugin.getBreakpointManager() != null) {
                        future.complete(plugin.getBreakpointManager());
                    } else {
                        Display.getDefault().timerExec(500, this);
                    }
                }
            };

            Display.getDefault().asyncExec(check);
            return future.join();
        }
    }

}
