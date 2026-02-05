package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractSimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;

/**
 * Processes debug events collected by the Simple Debugger.
 * Runs in a dedicated thread and dispatches events to the DebugWindow.
 */
public class SimpleDebugEventProcessor implements Runnable {

    private final DebugWindow debugWindow;

    /**
     * Creates a new debug event processor for the given debug window.
     *
     * @param debugWindow the window that will handle debug events
     */
    public SimpleDebugEventProcessor(DebugWindow debugWindow) {
        this.debugWindow = debugWindow;
    }

    /**
     * Continuously processes debug events from the global event queue.
     * This method blocks when no events are available and will only stop if the thread is interrupted.
     */
    @Override
    public void run() {
        while (!DebuggerContext.context().isInTerminalState()) {
            try {
                AbstractSimpleDebugEvent event = SimpleDebuggerEventQueue.instance().takeDebugEvent();
                SimpleDebuggerLogger.info("SimpleDebugEvent: " + event);
                debugWindow.handleDebugEvent(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // exit loop if interrupted
            }
        }
    }
}
