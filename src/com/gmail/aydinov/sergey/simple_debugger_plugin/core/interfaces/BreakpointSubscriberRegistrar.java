package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.BreakpointSubscriber;

@FunctionalInterface
public interface BreakpointSubscriberRegistrar {

	void register(BreakpointSubscriber subscriber);
}
