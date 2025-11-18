package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.sun.jdi.Location;

public interface BreakpointHitListener {
	void onBreakpointHit(Location location);
}
