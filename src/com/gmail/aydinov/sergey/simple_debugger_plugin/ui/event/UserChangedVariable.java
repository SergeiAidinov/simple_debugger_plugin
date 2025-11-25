package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.VarEntry;

public class UserChangedVariable extends UIEvent {
	
	private final VarEntry varEntry;

	public UserChangedVariable(VarEntry varEntry) {
		this.varEntry = varEntry;
	}

	public VarEntry getVarEntry() {
		return varEntry;
	}
	
}
