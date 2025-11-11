package com.gmail.aydinov.sergey.plugin_lifecycle;

import org.eclipse.ui.IStartup;

public class AppLifeCycle implements IStartup{

	  @Override
		public void earlyStartup() {
			 System.out.println("AppStartup сработал!");
		}
}