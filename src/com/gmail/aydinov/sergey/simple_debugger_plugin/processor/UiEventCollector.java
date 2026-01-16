package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import java.util.concurrent.TimeUnit;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractUIEvent;

public interface UiEventCollector {

	void collectUiEvent(AbstractUIEvent event);

	AbstractUIEvent pollUiEvent(long timeout, TimeUnit unit) throws InterruptedException;

}
