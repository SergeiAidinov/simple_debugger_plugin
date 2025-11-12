package com.gmail.aydinov.sergey.simple_debugger_plugin;

import java.util.Objects;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.swt.widgets.Display;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.BreakePointListener;

public class PluginStarter implements IStartup {

	private IBreakpointManager iBreakpointManager;

	@Override
	public void earlyStartup() {
		System.out.println("[AppLifeCycle] earlyStartup called.");

		// Выполнить регистрацию после инициализации UI
		Display.getDefault().asyncExec(() -> {
			try {
				DebugPlugin debugPlugin = DebugPlugin.getDefault();
				if (Objects.nonNull(debugPlugin) && Objects.nonNull(debugPlugin.getBreakpointManager())) {
					registerListener();
				} else {
					System.out.println("[AppLifeCycle] DebugPlugin not yet ready, retrying...");
					// Если DebugPlugin ещё не готов — повторим через секунду
					scheduleRetry();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void scheduleRetry() {
		Display.getDefault().timerExec(1000, () -> {
			DebugPlugin debugPlugin = DebugPlugin.getDefault();
			if (Objects.nonNull(debugPlugin) && Objects.nonNull(debugPlugin.getBreakpointManager())) {
				registerListener();
			} else {
				System.out.println("[AppLifeCycle] Still not ready, retrying again...");
				scheduleRetry();
			}
		});
	}

	private boolean registerListener() {
		iBreakpointManager = DebugPlugin.getDefault().getBreakpointManager();
		iBreakpointManager.setEnabled(true);
		BreakePointListener breakePointListener = new BreakePointListener();
		iBreakpointManager.addBreakpointListener(breakePointListener);
		System.out.println("[AppLifeCycle] Breakpoint listener registered!");
		return true;
	}
}
