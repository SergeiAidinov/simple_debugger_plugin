package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.UIEvent;

public interface UiEventProvider {
	
	void sendUiEvent(UIEvent uiEvent);

}
