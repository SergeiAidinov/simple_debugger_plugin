package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UIEvent;

public interface UiEventProvider {
	
	void sendUiEvent(UIEvent uiEvent);

}
