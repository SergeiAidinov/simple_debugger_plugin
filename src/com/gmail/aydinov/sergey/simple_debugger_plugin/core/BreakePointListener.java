package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.BreakpointSubscriber;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;

public class BreakePointListener implements IBreakpointListener, BreakpointSubscriberRegistrar {
	
	private BreakpointSubscriber breakpointSubscriber;

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
			try {
			if (!breakpoint.isEnabled())	breakpoint.setEnabled(true);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		printBreakpoint(breakpoint, "ADDED");
		breakpointSubscriber.addBreakepoint(breakpoint);
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
			try {
			if (breakpoint.isEnabled())	breakpoint.setEnabled(false);
			//breakpoint.delete();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		printBreakpoint(breakpoint, "REMOVED");
		breakpointSubscriber.deleteBreakepoint(breakpoint);	
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		System.out.println("breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta): breakpoint: " + breakpoint + " delta: " +  delta);		
		if (delta == null) {
	        return;
	    }

	    if ((delta.getKind() & IResourceDelta.REMOVED) != 0) {
	        System.out.println("Marker REMOVED for breakpoint: " + breakpoint);
	        return;
	    }

	    System.out.println("Breakpoint changed (but marker still exists)");
	
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


	@Override
	public void register(
			BreakpointSubscriber targetApplicationBreakepointRepresentation) {
		this.breakpointSubscriber = targetApplicationBreakepointRepresentation;
		
	}
}