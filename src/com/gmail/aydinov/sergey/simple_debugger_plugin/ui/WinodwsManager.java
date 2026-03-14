package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window.ManageableCollectionInspectorWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.main_window.ManageableMainWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.universal_inspector_window.UniversalInspectorWindow;

interface WinodwsManager {

	ManageableMainWindow getOrCreateMainWindow();
	
	//ManageableCollectionInspectorWindow getManageableCollectionInspectorWindowFor();
	
	UniversalInspectorWindow getUniversalInspectorWindow();
	UniversalInspectorWindow getUniversalInspectorWindowFor(InnerElementRepresentationDTO innerElementRepresentationDTO);
}
