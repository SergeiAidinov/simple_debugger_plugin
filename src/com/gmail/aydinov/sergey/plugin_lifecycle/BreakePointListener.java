package com.gmail.aydinov.sergey.plugin_lifecycle;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.model.IBreakpoint;

public class BreakePointListener implements IBreakpointListener {

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		System.out.println("Something happend");
		
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		System.out.println("Something happend");		
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		System.out.println("Something happend");		
	}


}
