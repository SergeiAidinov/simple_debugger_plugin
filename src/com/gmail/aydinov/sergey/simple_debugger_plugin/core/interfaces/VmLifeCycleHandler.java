package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

@FunctionalInterface
public interface VmLifeCycleHandler {
    void handleVmStopped();
}
