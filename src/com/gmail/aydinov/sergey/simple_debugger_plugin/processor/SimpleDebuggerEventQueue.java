package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.SimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.UIEvent;

public class SimpleDebuggerEventQueue implements UiEventCollector, SimpleDebugEventCollector {

	private static SimpleDebuggerEventQueue INSTANCE = null;

	private SimpleDebuggerEventQueue() {
	}

	public static SimpleDebuggerEventQueue instance() {
		if (Objects.isNull(INSTANCE))
			INSTANCE = new SimpleDebuggerEventQueue();
		return INSTANCE;
	}

	private final BlockingQueue<UIEvent> uIEventQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<SimpleDebugEvent> debugEventQueue = new LinkedBlockingQueue<>();

	@Override
	public void collectUiEvent(UIEvent event) {
		uIEventQueue.offer(event); // offer не блокирует
	}

	// Получить событие для обработки (Worker поток)
	@Override
	public UIEvent takeUiEvent() throws InterruptedException {
		return uIEventQueue.take(); // блокируется, пока нет событий
	}

	@Override
	public void collectDebugEvent(SimpleDebugEvent event) {
		debugEventQueue.offer(event); // offer не блокирует
	}

	// Получить событие для обработки (Worker поток)
	@Override
	public SimpleDebugEvent takeDebugEvent() throws InterruptedException {
		return debugEventQueue.take(); // блокируется, пока нет событий
	}
}
