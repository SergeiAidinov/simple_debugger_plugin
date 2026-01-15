package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.InvokeMethodEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserClosedWindowUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserPressedResumeUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserPressedStartUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.*;
import com.sun.jdi.event.*;

public class DebugSessionImpl implements DebugSession {

    private enum DebugState {
        RUNNING,
        SUSPENDED,
        TERMINATED
    }

    private static final AtomicReference<String> resultOfMethodInvocation = new AtomicReference<>("");

    private final TargetVirtualMachineRepresentation vmRep;
    private final TargetApplicationRepresentation appRep;
    private final EventSet eventSet;
    private final CurrentLineHighlighter highlighter;

    private volatile DebugState debugState = DebugState.RUNNING;

    public DebugSessionImpl(
            TargetVirtualMachineRepresentation vmRep,
            TargetApplicationRepresentation appRep,
            EventSet eventSet,
            CurrentLineHighlighter highlighter
    ) {
        this.vmRep = vmRep;
        this.appRep = appRep;
        this.eventSet = eventSet;
        this.highlighter = highlighter;
    }

    @Override
    public void run() {
        try {
            System.out.println("DEBUG SESSION STARTED: " + LocalDateTime.now());
            processEventSet();
        } catch (Throwable t) {
            logError("Fatal error in debug session", t);
        } finally {
            System.out.println("DEBUG SESSION FINISHED: " + LocalDateTime.now());
        }
    }

    private void processEventSet() {
        for (Event event : eventSet) {

            if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
                handleVmDisconnected();
                return;
            }

            if (event instanceof BreakpointEvent bp) {
                debugState = DebugState.SUSPENDED;
                onBreakpoint(bp);
            }
        }

        if (debugState == DebugState.RUNNING) {
            eventSet.resume();
        }
    }

    private void onBreakpoint(BreakpointEvent bp) {
        setHighlight(bp);
        refreshUI(bp);
    }

    /* ========================= UI EVENTS ========================= */

    public void handleUiEvent(UIEvent uiEvent) {
        try {
            if (uiEvent instanceof UserPressedResumeUiEvent) {
                resume();
            } else if (uiEvent instanceof UserPressedStartUiEvent) {
                System.out.println("Start pressed (noop for now)");
            } else if (uiEvent instanceof UserClosedWindowUiEvent) {
                terminate();
            } else if (uiEvent instanceof UserChangedVariableDTO dto) {
                updateVariable(dto);
            } else if (uiEvent instanceof UserChangedFieldDTO dto) {
                updateField(dto);
            } else if (uiEvent instanceof InvokeMethodEvent evt) {
                invokeMethod(evt);
            }
        } catch (Exception e) {
            logError("UI event handling failed", e);
        }
    }

    private void resume() {
        debugState = DebugState.RUNNING;
       // clearHighlight();
        vmRep.getVirtualMachine().resume();
    }

    private void terminate() {
        debugState = DebugState.TERMINATED;
        DebuggerContext.context().setRunning(false);
        vmRep.getVirtualMachine().dispose();
    }

    /* ========================= DATA OPS ========================= */

    private void updateVariable(UserChangedVariableDTO dto) throws Exception {
        ThreadReference thread = vmRep.getVirtualMachine().allThreads().get(0);
        StackFrame frame = thread.frame(0);

        LocalVariable var = frame.visibleVariables().stream()
                .filter(v -> v.name().equals(dto.getName()))
                .findFirst()
                .orElse(null);

        if (var == null) return;

        Value value = DebugUtils.createJdiValueFromString(
                vmRep.getVirtualMachine(), var, dto.getNewValue().toString()
        );
        frame.setValue(var, value);
    }

    private void updateField(UserChangedFieldDTO dto) throws Exception {
        ThreadReference thread = vmRep.getVirtualMachine().allThreads().get(0);
        StackFrame frame = thread.frame(0);

        ReferenceType refType = frame.thisObject() != null
                ? frame.thisObject().referenceType()
                : frame.location().declaringType();

        Field field = refType.fieldByName(dto.getFieldName());
        if (field == null) return;

        Value val = DebugUtils.createJdiObjectFromString(
                vmRep.getVirtualMachine(), field.type(), dto.getNewValue(), thread
        );

        if (field.isStatic() && refType instanceof ClassType ct) {
            ct.setValue(field, val);
        } else if (frame.thisObject() != null) {
            frame.thisObject().setValue(field, val);
        }
    }

    private void invokeMethod(InvokeMethodEvent event) throws Exception {
        ReferenceType refType = appRep.findReferenceTypeForClass(event.getClazz());
        Method method = refType.methodsByName(event.getMethod().getMethodName()).get(0);

        List<Value> args = DebugUtils.parseArguments(vmRep.getVirtualMachine(), event);
        ThreadReference thread = vmRep.getVirtualMachine().allThreads().get(0);

        Value result = method.isStatic()
                ? ((ClassType) refType).invokeMethod(thread, method, args, ClassType.INVOKE_SINGLE_THREADED)
                : appRep.createObjectInstance((ClassType) refType)
                        .invokeMethod(thread, method, args, ObjectReference.INVOKE_SINGLE_THREADED);

        resultOfMethodInvocation.set(String.valueOf(result));
    }

    /* ========================= UI ========================= */

    private void refreshUI(BreakpointEvent bp) {
        StackFrame frame;
        try {
            frame = bp.thread().frame(0);
        } catch (Exception e) {
            return;
        }

        Location loc = bp.location();

        SimpleDebugEventDTO dto = new SimpleDebugEventDTO.Builder()
                .type(SimpleDebugEventType.REFRESH_DATA)
                .className(loc.declaringType().name())
                .methodName(loc.method().name())
                .lineNumber(loc.lineNumber())
                .fields(DebugUtils.mapFields(DebugUtils.compileFields(frame)))
                .locals(DebugUtils.mapLocals(DebugUtils.compileLocalVariables(frame)))
                .methodCallInStacks(DebugUtils.compileStackInfo(bp.thread()))
                .resultOfMethodInvocation(resultOfMethodInvocation.get())
                .targetApplicationElementRepresentationList(appRep.getTargetApplicationElements())
                .build();

        SimpleDebuggerEventQueue.instance().collectDebugEvent(dto);
    }

    private void setHighlight(BreakpointEvent bp) {
        Display.getDefault().asyncExec(() -> {
            try {
                ITextEditor editor = openEditor(bp.location());
                if (editor != null) {
                    highlighter.highlight(editor, bp.location().lineNumber() - 1);
                }
            } catch (Exception e) {
                logError("Highlight failed", e);
            }
        });
    }

//    private void clearHighlight() {
//        Display.getDefault().asyncExec(highlighter::clearPreviousHighlight);
//    }

    private ITextEditor openEditor(Location location) throws Exception {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) return null;

        IWorkbenchPage page = window.getActivePage();
        if (page == null) return null;

        IFile file = appRep.findIFileForLocation(location);
        if (file == null) return null;

        IEditorPart part = IDE.openEditor(page, file, true);
        return part instanceof ITextEditor te ? te : null;
    }

    private void handleVmDisconnected() {
        try {
            appRep.detachDebugger();
            eventSet.resume();
        } catch (Exception ignored) {}
    }

    @Override
    public void stop() {
        terminate();
    }

    private void logError(String msg, Throwable t) {
        StatusManager.getManager().handle(
                new Status(IStatus.ERROR, "simple_debugger_plugin", msg, t),
                StatusManager.LOG
        );
    }
}
