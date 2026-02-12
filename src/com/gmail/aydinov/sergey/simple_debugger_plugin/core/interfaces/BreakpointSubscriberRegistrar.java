package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.BreakpointSubscriber;

/**
 * Functional interface for registering a breakpoint subscriber.
 */
@FunctionalInterface
public interface BreakpointSubscriberRegistrar {

    /**
     * Registers the given breakpoint subscriber.
     *
     * @param subscriber the subscriber to register
     */
    void register(BreakpointSubscriber subscriber);
}
