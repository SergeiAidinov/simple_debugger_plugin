package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window.tab;

import java.util.List;

import org.eclipse.swt.widgets.Composite;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.CollectionEntryDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public interface InspectorTab {
    Composite getControl();
	void showCollection(List<InnerElementRepresentationDTO> elements);
}