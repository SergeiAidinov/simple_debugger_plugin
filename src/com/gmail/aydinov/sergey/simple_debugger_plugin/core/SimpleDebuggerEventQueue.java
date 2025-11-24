package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.SimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UIEvent;


public class SimpleDebuggerEventQueue {
	
	private static SimpleDebuggerEventQueue INSTANCE = null;
	
    private SimpleDebuggerEventQueue() {
	}
    
    public static SimpleDebuggerEventQueue instance() {
    	if (Objects.isNull(INSTANCE)) INSTANCE = new SimpleDebuggerEventQueue();
    	return INSTANCE;
    }

	private final BlockingQueue<UIEvent> uIEventQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<SimpleDebugEvent> debugEventQueue = new LinkedBlockingQueue<>();

    // Добавить событие в очередь (UI поток)
    public void addUiEvent(UIEvent event) {
        uIEventQueue.offer(event); // offer не блокирует
    }

    // Получить событие для обработки (Worker поток)
    public UIEvent takeUiEvent() throws InterruptedException {
        return uIEventQueue.take(); // блокируется, пока нет событий
    }
    
    public void addDebugEvent(SimpleDebugEvent event) {
        debugEventQueue.offer(event); // offer не блокирует
    }

    // Получить событие для обработки (Worker поток)
    public SimpleDebugEvent takeDebugEvent() throws InterruptedException {
        return debugEventQueue.take(); // блокируется, пока нет событий
    }
}
