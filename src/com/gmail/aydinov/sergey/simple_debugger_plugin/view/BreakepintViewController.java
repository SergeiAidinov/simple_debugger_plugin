package com.gmail.aydinov.sergey.simple_debugger_plugin.view;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.BreakpointHitListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.TargetApplicationBreakepointRepresentation;
import com.sun.jdi.Location;

public class BreakepintViewController {

	private static BreakepintViewController instance = null;
	private BreakpointHitListener breakpointHitListener;
	private TargetApplicationBreakepointRepresentation targetApplicationBreakepointRepresentation;

	private BreakepintViewController() {
	}

	public static BreakepintViewController instance() {
		if (Objects.isNull(instance))
			instance = new BreakepintViewController();
		return instance;
	}

	public BreakpointHitListener getBreakpointHitListener() {
		return breakpointHitListener;
	}

	public void setBreakpointHitListener(BreakpointHitListener breakpointHitListener) {
		this.breakpointHitListener = breakpointHitListener;
	}

	public TargetApplicationBreakepointRepresentation getTargetApplicationBreakepointRepresentation() {
		return targetApplicationBreakepointRepresentation;
	}

	public void setTargetApplicationBreakepointRepresentation(
			TargetApplicationBreakepointRepresentation targetApplicationBreakepointRepresentation) {
		this.targetApplicationBreakepointRepresentation = targetApplicationBreakepointRepresentation;
	}

	public void fireBreakpointHit(Location loc) {
		if (Objects.nonNull(breakpointHitListener))
			breakpointHitListener.onBreakpointHit(loc);
	}

	public List<Location> getAllLocations() {
		return targetApplicationBreakepointRepresentation.getLocations().stream().toList();
	}

}
