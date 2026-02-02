package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

/**
 * Interface for handling the lifecycle events of a target Virtual Machine.
 */
@FunctionalInterface
public interface VmLifeCycleHandler {

    /**
     * Called when the target Virtual Machine has stopped.
     */
    void handleVmStopped();
}
