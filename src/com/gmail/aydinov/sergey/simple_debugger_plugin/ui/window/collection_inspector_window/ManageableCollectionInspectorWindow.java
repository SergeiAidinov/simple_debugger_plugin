package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.collection_inspector_window;

import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.ManageableWindow;

public interface ManageableCollectionInspectorWindow extends ManageableWindow{
	
	 static ManageableCollectionInspectorWindow getOrCreateCollectionInspectWindow() {
		return CollectionInspectorWindow.getOrCreateInspectWindow();
	 }

	

}
