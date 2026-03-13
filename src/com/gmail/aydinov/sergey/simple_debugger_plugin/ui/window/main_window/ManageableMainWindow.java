package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.main_window;

import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.ManageableWindow;

public interface ManageableMainWindow extends ManageableWindow {

	static ManageableMainWindow getOrCreateMainWindow() {
		return MainWindow.getOrCreateMainWindow();
	}

	void showError(String string, String message);

}
