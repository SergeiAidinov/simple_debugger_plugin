package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

public interface UiEventCollector {

	void collectUiEvent(AbstractUIEvent event);
	
	public AbstractUIEvent pollUiEvent() throws InterruptedException;

}
