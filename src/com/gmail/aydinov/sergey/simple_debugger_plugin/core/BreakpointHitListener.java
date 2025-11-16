package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.sun.jdi.request.BreakpointRequest;

public interface BreakpointHitListener {

	void registerTargetApplicationBreakepointRepresentation(
			TargetApplicationBreakepointRepresentation targetApplicationBreakepointRepresentation);
}
