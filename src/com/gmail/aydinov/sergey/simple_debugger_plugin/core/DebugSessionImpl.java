package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.*;
import com.sun.jdi.*;

import com.sun.jdi.event.*;

public class DebugSessionImpl implements DebugSession {

    private final VirtualMachine vm;
    private final BreakpointEventHandler breakpointHandler;
    private final VmLifeCycleHandler vmLifeCycleHandler;
    private final TargetApplicationRepresentation targetApplication;
    private final UIRefresher uiRefresher;
    private final CurrentLineHighlighter highlighter = new CurrentLineHighlighter();
    private final EventSet eventSet;

    private volatile boolean running = true;
    private volatile boolean started = false;

    private Thread loopThread;

    public DebugSessionImpl(
        VirtualMachine vm,
        BreakpointEventHandler breakpointHandler,
        VmLifeCycleHandler vmLifeCycleHandler,
        UIRefresher uiRefresher,
        TargetApplicationRepresentation targetApplication,
        EventSet eventSet
    ) {
        this.vm = vm;
        this.breakpointHandler = breakpointHandler;
        this.vmLifeCycleHandler = vmLifeCycleHandler;
        this.uiRefresher = uiRefresher;
        this.targetApplication = targetApplication;
        this.eventSet = eventSet;
    }

//    @Override
//    public synchronized void runLoop() {
//        if (started) return;
//
//        started = true;
//        loopThread = new Thread(this, "JDI-EventLoop-Thread");
//        loopThread.setDaemon(true);
//        loopThread.start();
//    }

    @Override
    public void run() {
        try {
        	System.out.println("RUN");
            process();
        } catch (Throwable t) {
            logError("Fatal error in JDI event loop", t);
        }
    }

    private void process() {
       
		for (Event event : eventSet ) {
			if (event instanceof BreakpointEvent bp) {
                handleBreakpoint(bp);
            } else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
                handleVmDisconnected();
                return;
            }
		}
	
}
		

    private void handleBreakpoint(BreakpointEvent bp) {
        // 1. Обновить представленную модель
        try {
            targetApplication.refreshReferencesToClassesOfTargetApplication(vm);
        } catch (Throwable t) {
            logError("Cannot refresh target application representation", t);
        }

        // 2. Подсветить строку в редакторе
        Display display = Display.getDefault();
        if (display != null && !display.isDisposed()) {
            display.asyncExec(() -> {
                try {
                    ITextEditor editor = openEditorForLocation(bp.location());
                    if (editor != null) {
                        int line = bp.location().lineNumber() - 1;
                        highlighter.highlight(editor, line);
                    }
                } catch (Throwable t) {
                    logError("Cannot highlight breakpoint location", t);
                }
            });
        }
        
        uiRefresher.refreshUI(bp);

        // 3. Логика обработчика
        try {
            breakpointHandler.handle(bp);
        } catch (Throwable t) {
            logError("Breakpoint handler error", t);
        }
    }

    private ITextEditor openEditorForLocation(Location location) throws Exception {
        if (location == null) return null;

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) return null;

        IWorkbenchPage page = window.getActivePage();
        if (page == null) return null;

        IFile file = targetApplication.findIFileForLocation(location);
        if (file == null)
            throw new IllegalStateException("Cannot map location to IFile: " + location);

        IEditorPart part = IDE.openEditor(page, file, true);
        if (part instanceof ITextEditor editor) {
            return editor;
        }
        throw new IllegalStateException("Opened editor is not a text editor");
    }

    private void handleVmDisconnected() {
        try {
            vmLifeCycleHandler.handleVmStopped();
        } catch (Throwable t) {
            logError("VM lifecycle handler error", t);
        }
        running = false;
    }

    @Override
    public void stop() {
        running = false;
        if (loopThread != null)
            loopThread.interrupt();
    }

    private void logError(String message, Throwable t) {
        StatusManager.getManager().handle(
            new Status(IStatus.ERROR, "simple_debugger_plugin", message, t),
            StatusManager.LOG
        );
    }

}
