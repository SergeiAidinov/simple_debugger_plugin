package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.BreakpointSubscriber;

public interface BreakpointSubscriberRegistrar {

	void register(BreakpointSubscriber subscriber);
}
