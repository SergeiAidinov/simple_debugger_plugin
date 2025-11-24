package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UIEvent;


public class UiEventQueue {
	
	private static UiEventQueue INSTANCE = null;
	
    private UiEventQueue() {
	}
    
    public static UiEventQueue instance() {
    	if (Objects.isNull(INSTANCE)) INSTANCE = new UiEventQueue();
    	return INSTANCE;
    }

	private final BlockingQueue<UIEvent> queue = new LinkedBlockingQueue<>();

    // Добавить событие в очередь (UI поток)
    public void addEvent(UIEvent event) {
        queue.offer(event); // offer не блокирует
    }

    // Получить событие для обработки (Worker поток)
    public UIEvent takeEvent() throws InterruptedException {
        return queue.take(); // блокируется, пока нет событий
    }
}
