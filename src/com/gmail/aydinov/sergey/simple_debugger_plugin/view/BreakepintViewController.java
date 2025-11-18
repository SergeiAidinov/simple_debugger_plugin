package com.gmail.aydinov.sergey.simple_debugger_plugin.view;

import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.BreakpointHitListener;
import com.sun.jdi.Location;

public class BreakepintViewController {
	
	private static BreakepintViewController instance = null;
	private BreakpointHitListener breakpointHitListener;

	private BreakepintViewController() {
	}
	
	public static BreakepintViewController instance() {
		if (Objects.isNull(instance)) instance = new BreakepintViewController();
		return instance;
	}

	public BreakpointHitListener getBreakpointHitListener() {
		return breakpointHitListener;
	}

	public void setBreakpointHitListener(BreakpointHitListener breakpointHitListener) {
		this.breakpointHitListener = breakpointHitListener;
	}

	public void fireBreakpointHit(Location loc) {
		if (Objects.nonNull(breakpointHitListener)) breakpointHitListener.onBreakpointHit(loc);
		
	}

}
