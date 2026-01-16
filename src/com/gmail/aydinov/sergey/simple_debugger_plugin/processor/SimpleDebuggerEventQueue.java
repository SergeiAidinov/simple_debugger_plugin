package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractSimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugStoppedAtBreakepointEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

public class SimpleDebuggerEventQueue implements UiEventCollector, SimpleDebugEventCollector {

	private static SimpleDebuggerEventQueue INSTANCE = null;

	private SimpleDebuggerEventQueue() {
	}

	public static SimpleDebuggerEventQueue instance() {
		if (Objects.isNull(INSTANCE))
			INSTANCE = new SimpleDebuggerEventQueue();
		return INSTANCE;
	}

	//private final Queue<UIEvent> uIEventQueue = new ConcurrentLinkedQueue<>();
	private final BlockingQueue<AbstractUIEvent> uIEventQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<AbstractSimpleDebugEvent> debugEventQueue = new LinkedBlockingQueue<>();

	@Override
	public void collectUiEvent(AbstractUIEvent event) {
		uIEventQueue.offer(event); 
	}

	@Override
	public void collectDebugEvent(AbstractSimpleDebugEvent event) {
		debugEventQueue.offer(event); // offer не блокирует
	}

	// Получить событие для обработки (Worker поток)
	@Override
	public AbstractSimpleDebugEvent takeDebugEvent() throws InterruptedException {
		return debugEventQueue.take(); // блокируется, пока нет событий
	}
	
	public AbstractUIEvent takeUiEvent() throws InterruptedException {
		return uIEventQueue.take(); // блокируется, пока нет событий
	}
	
	public AbstractUIEvent pollUiEvent(long timeout, TimeUnit unit) throws InterruptedException {
        return uIEventQueue.poll(timeout, unit);
    }
	
	public AbstractUIEvent pollUiEvent() throws InterruptedException {
        return uIEventQueue.poll();
    }
}
