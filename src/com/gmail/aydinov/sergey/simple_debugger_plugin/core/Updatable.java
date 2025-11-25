package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.UIEventUpdateVariable;

public interface Updatable {
	
	void updateVariables(UIEventUpdateVariable uiEventUpdateVariable);

}
