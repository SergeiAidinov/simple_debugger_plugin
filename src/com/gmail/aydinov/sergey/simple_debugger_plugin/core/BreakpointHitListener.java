package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.sun.jdi.Location;
import com.sun.jdi.ThreadReference;

public interface BreakpointHitListener {
	void onBreakpointHit(Location location);

	void onBreakpointHit(Location location, ThreadReference thread);
}
