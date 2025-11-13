package com.gmail.aydinov.sergey.simple_debugger_plugin;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.BreakePointListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow;
import com.sun.jdi.AbsentInformationException;

public class PluginStarter implements IStartup {

	private SimpleDebuggerWorkFlow simpleDebuggerWorkFlow;
	// private final CountDownLatch latch = new CountDownLatch(2);

	@Override
	public void earlyStartup() {
		System.out.println("[AppLifeCycle] earlyStartup called.");
		simpleDebuggerWorkFlow = SimpleDebuggerWorkFlow.Factory.create("localhost", 8000);
		try {
			simpleDebuggerWorkFlow.debug();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AbsentInformationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.err.println("[DEBUG] earlyStartup reached end");

	}
}
