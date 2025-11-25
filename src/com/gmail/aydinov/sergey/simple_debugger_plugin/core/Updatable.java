package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.event.UserChangedVariable;

public interface Updatable {
	
	void updateVariables(UserChangedVariable userChangedVariable);

}
