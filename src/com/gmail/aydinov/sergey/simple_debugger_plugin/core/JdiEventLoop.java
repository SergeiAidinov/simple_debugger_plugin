package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.EventLoop;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.TargetApplicationStatusProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.VmLifeCycleHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.sun.jdi.Location;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class JdiEventLoop implements Runnable, EventLoop {

    private final VirtualMachine vm;
    private final BreakpointEventHandler breakpointHandler;
    private final VmLifeCycleHandler vmLifeCycleHandler;
    private final TargetApplicationRepresentation targetApplication;
    private final TargetApplicationStatusProvider targetApplicationStatusProvider;
    private final CurrentLineHighlighter highlighter = new CurrentLineHighlighter();

    private volatile boolean running = true;
    private boolean started = false;

    public JdiEventLoop(VirtualMachine vm,
                        BreakpointEventHandler breakpointHandler,
                        VmLifeCycleHandler vmLifeCycleHandler,
                        TargetApplicationRepresentation targetApplication,
                        TargetApplicationStatusProvider targetApplicationStatusProvider) {
        this.vm = vm;
        this.breakpointHandler = breakpointHandler;
        this.vmLifeCycleHandler = vmLifeCycleHandler;
        this.targetApplication = targetApplication;
        this.targetApplicationStatusProvider = targetApplicationStatusProvider;
    }

    @Override
    public void runLoop() {
        if (!started) {
            started = true;
            Thread loopThread = new Thread(this, "JDI-EventLoop-Thread");
            loopThread.setDaemon(true);
            loopThread.start();
        }
    }

    @Override
    public void run() {
        EventQueue queue = vm.eventQueue();

        while (running) {
            EventSet set;
            try {
                set = queue.remove(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

            if (!running || set == null) continue;

            try {
                for (Event ev : set) {
                    if (ev instanceof BreakpointEvent bp) {
                        handleBreakpoint(bp);
                    } else if (ev instanceof VMDisconnectEvent || ev instanceof VMDeathEvent) {
                        vmLifeCycleHandler.handleVmStopped();
                        running = false;
                        break;
                    }
                }
            } finally {
                try { set.resume(); } catch (Exception ignore) {}
            }
        }
    }

    private void handleBreakpoint(BreakpointEvent bp) {
        // 1. Обновляем представление классов и объектов
        targetApplication.refreshReferencesToClassesOfTargetApplication(vm);

        // 2. Подсвечиваем строку в Eclipse
        Display.getDefault().asyncExec(() -> {
            try {
                ITextEditor editor = openEditorForLocation(bp.location());
                if (editor != null) {
                    highlighter.highlight(editor, bp.location().lineNumber() - 1);
                }
            } catch (Exception ignored) {}
        });

        // 3. Вызываем обработчик брейкпоинта для остальной логики
        breakpointHandler.handle(bp);
    }

    private ITextEditor openEditorForLocation(Location location) throws Exception {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        IFile file = targetApplication.findIFileForLocation(location);
        IEditorPart part = IDE.openEditor(page, file, true);
        if (!(part instanceof ITextEditor editor)) throw new IllegalStateException("Not a text editor");
        return editor;
    }

    @Override
    public void stop() {
        running = false;
    }
}
