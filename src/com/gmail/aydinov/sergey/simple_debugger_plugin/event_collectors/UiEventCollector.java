package com.gmail.aydinov.sergey.simple_debugger_plugin.event_collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractSimpleDebuggerUIEvent;

/**
 * Interface for collecting UI events produced by the user interface.
 * Implementations typically store events in a queue for later processing by worker threads.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public interface UiEventCollector {

    /**
     * Collects a UI event and adds it to the underlying event queue.
     * Non-blocking operation.
     *
     * @param event the UI event to collect
     */
    void collectUiEvent(AbstractSimpleDebuggerUIEvent event);

    /**
     * Retrieves and removes the next UI event from the queue.
     * If the queue is empty, this method may return null or wait, depending on implementation.
     *
     * @return the next UI event, or null if none is available
     * @throws InterruptedException if the operation is interrupted while waiting
     */
    AbstractSimpleDebuggerUIEvent pollUiEvent();
    
    AbstractSimpleDebuggerUIEvent takeUiEvent() throws InterruptedException;
}
