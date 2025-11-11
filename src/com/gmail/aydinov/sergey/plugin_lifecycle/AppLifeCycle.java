package com.gmail.aydinov.sergey.plugin_lifecycle;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

public class AppLifeCycle implements IStartup {

	private IBreakpointManager iBreakpointManager;

	@Override
	public void earlyStartup() {
		System.out.println("AppLifeCycle: earlyStartup() called!");
		try {
			Thread.currentThread().sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
			
			iBreakpointManager = DebugPlugin.getDefault().getBreakpointManager();
			iBreakpointManager.setEnabled(true);
			BreakePointListener breakePointListener = new BreakePointListener();
			iBreakpointManager.addBreakpointListener(breakePointListener);
		
	}

	private void printBreakpoint(IBreakpoint bp, String action) {
		try {
			IResource resource = bp.getMarker().getResource();
			String projectName = (resource != null && resource.getProject() != null) ? resource.getProject().getName()
					: "Unknown Project";
			String fileName = (resource != null) ? resource.getName() : "Unknown File";
			int lineNumber = bp.getMarker().getAttribute("lineNumber", -1);
			boolean enabled = bp.isEnabled();

			System.out.println(action + " -> проект: " + projectName + ", файл: " + fileName + ", строка: " + lineNumber
					+ ", включён: " + enabled);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
