package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.events.UIEvent;

public interface UiEventCollector {

	void collectUiEvent(UIEvent event);

	UIEvent takeUiEvent() throws InterruptedException;

}
