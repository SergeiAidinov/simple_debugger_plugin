package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.UIEvent;

public interface UiEventCollector {

	void collectUiEvent(UIEvent event);

	UIEvent takeUiEvent() throws InterruptedException;

}
