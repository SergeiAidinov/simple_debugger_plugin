package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.inspect_window_tab;

import org.eclipse.swt.widgets.Composite;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;

public interface InspectorTab {

	Composite getControl();

	void showPage(AbstractInspectionDTO page);

}