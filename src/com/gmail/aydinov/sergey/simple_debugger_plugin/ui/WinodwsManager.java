package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window.ManageableCollectionInspectorWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.main_window.ManageableMainWindow;

interface WinodwsManager {

	ManageableMainWindow getOrCreateMainWindow();
	
	//ManageableCollectionInspectorWindow getManageableCollectionInspectorWindowFor();
	
	ManageableCollectionInspectorWindow getManageableCollectionInspectorWindow();

	ManageableCollectionInspectorWindow getManageableCollectionInspectorWindowFor(
			InnerElementRepresentationDTO innerElementRepresentationDTO);
}
