package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import java.util.concurrent.TimeUnit;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UIEvent;

public interface UiEventCollector {

	void collectUiEvent(UIEvent event);

	UIEvent pollUiEvent(long timeout, TimeUnit unit) throws InterruptedException;

}
