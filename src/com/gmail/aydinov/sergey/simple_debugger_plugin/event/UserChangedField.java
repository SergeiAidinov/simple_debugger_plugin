package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.FieldEntry;

public class UserChangedField extends UIEvent{
	
	private final FieldEntry fieldEntry;

	public UserChangedField(FieldEntry fieldEntry) {
		this.fieldEntry = fieldEntry;
	}

	public FieldEntry getFieldEntry() {
		return fieldEntry;
	}
	
}
