//package com.gmail.aydinov.sergey.simple_debugger_plugin;
//
//import org.eclipse.ui.IStartup;
//
//import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow;
//
//public class PluginStarter implements IStartup {
//	
//	@Override
//	public void earlyStartup() {
//		System.out.println("[AppLifeCycle] earlyStartup called.");
//		SimpleDebuggerWorkFlow.Factory.create("localhost", 8000, workflow -> {
//			System.out.println("[AppLifeCycle] Workflow ready! Starting debug...");
//			try {
//				workflow.debug();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		});
//	}
//}
