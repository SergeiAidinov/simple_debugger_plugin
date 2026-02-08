package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow;

/**
 * Listener interface invoked when a {@link SimpleDebuggerWorkFlow} is fully initialized
 * and ready to start debugging.
 */
@FunctionalInterface
public interface OnWorkflowReadyListener {

    /**
     * Called when the debug workflow is ready.
     *
     * @param workflow the initialized {@link SimpleDebuggerWorkFlow} instance
     */
    void onReady(SimpleDebuggerWorkFlow workflow);
}
