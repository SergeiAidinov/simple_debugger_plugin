package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractSimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

/**
 * Singleton queue for handling UI and Debug events in the Simple Debugger.
 * Supports multiple producers (UI or Debug threads) and consumers (Worker threads).
 */
public class SimpleDebuggerEventQueue implements UiEventCollector, SimpleDebugEventCollector {

    private static SimpleDebuggerEventQueue INSTANCE = null;

    /** Private constructor to enforce singleton pattern */
    private SimpleDebuggerEventQueue() { }

    /**
     * Returns the singleton instance of the event queue.
     *
     * @return the single instance of SimpleDebuggerEventQueue
     */
    public static synchronized SimpleDebuggerEventQueue instance() {
        if (Objects.isNull(INSTANCE)) {
            INSTANCE = new SimpleDebuggerEventQueue();
        }
        return INSTANCE;
    }

    private final BlockingQueue<AbstractUIEvent> uiEventQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<AbstractSimpleDebugEvent> debugEventQueue = new LinkedBlockingQueue<>();

    /**
     * Collects a UI event from the UI thread or other producers.
     * Non-blocking.
     *
     * @param event the UI event to add
     */
    @Override
    public void collectUiEvent(AbstractUIEvent event) {
        uiEventQueue.offer(event);
    }

    /**
     * Collects a debug event from the debugger or other producers.
     * Non-blocking.
     *
     * @param event the debug event to add
     */
    @Override
    public void collectDebugEvent(AbstractSimpleDebugEvent event) {
        debugEventQueue.offer(event);
    }

    /**
     * Retrieves and removes the next debug event, waiting if necessary.
     *
     * @return the next debug event
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public AbstractSimpleDebugEvent takeDebugEvent() throws InterruptedException {
        return debugEventQueue.take();
    }

    /**
     * Retrieves and removes the next UI event, or returns null if none is available.
     *
     * @return the next UI event, or null if queue is empty
     * @throws InterruptedException ignored here because poll() is non-blocking
     */
    @Override
    public AbstractUIEvent pollUiEvent() {
        return uiEventQueue.poll();
    }
}
