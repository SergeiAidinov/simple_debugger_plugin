package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

public class BreakePointListener implements IBreakpointListener {

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		printBreakpoint(breakpoint, "ADDED");
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		printBreakpoint(breakpoint, "REMOVED");	
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		printBreakpoint(breakpoint, "CHANGED");		
	}
	
	private void printBreakpoint(IBreakpoint breakpoint, String action) {
		try {
			IResource resource = breakpoint.getMarker().getResource();
			String projectName = (resource != null && resource.getProject() != null) ? resource.getProject().getName()
					: "Unknown Project";
			String fileName = (resource != null) ? resource.getName() : "Unknown File";
			int lineNumber = breakpoint.getMarker().getAttribute("lineNumber", -1);
			boolean enabled = breakpoint.isEnabled();

			System.out.println(action + " -> проект: " + projectName + ", файл: " + fileName + ", строка: " + lineNumber
					+ ", включён: " + enabled);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
