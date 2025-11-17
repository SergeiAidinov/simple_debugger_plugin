package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

public interface BreakpointRegistrationListener {

	void register(
			BreakpointSubscriber targetApplicationBreakepointRepresentation);
}
