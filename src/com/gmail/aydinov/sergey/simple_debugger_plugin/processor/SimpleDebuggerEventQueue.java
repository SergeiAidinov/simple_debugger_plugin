package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UIEvent;

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
	private final BlockingQueue<UIEvent> uIEventQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<SimpleDebugEventDTO> debugEventQueue = new LinkedBlockingQueue<>();

	@Override
	public void collectUiEvent(UIEvent event) {
		uIEventQueue.offer(event); 
	}

	@Override
	public void collectDebugEvent(SimpleDebugEventDTO event) {
		debugEventQueue.offer(event); // offer не блокирует
	}

	// Получить событие для обработки (Worker поток)
	@Override
	public SimpleDebugEventDTO takeDebugEvent() throws InterruptedException {
		return debugEventQueue.take(); // блокируется, пока нет событий
	}
	
	public UIEvent pollUiEvent(long timeout, TimeUnit unit) throws InterruptedException {
        return uIEventQueue.poll(timeout, unit);
    }
	
	public UIEvent pollUiEvent() throws InterruptedException {
        return uIEventQueue.poll();
    }
}
