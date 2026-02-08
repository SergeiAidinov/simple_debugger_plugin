package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;

/**
 * Event representing the result of a method invocation in the target application.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class BackendMethodExecutedEvent extends AbstractSimpleDebugEvent {

    private final String resultOfInvocation;

    /**
     * Creates a new MethodInvokedEvent.
     *
     * @param type the type of debugger event
     * @param resultOfInvocation the result returned by the invoked method
     */
    public BackendMethodExecutedEvent(SimpleDebuggerEventType type, String resultOfInvocation) {
        super(type);
        this.resultOfInvocation = resultOfInvocation;
    }

    /**
     * Returns the result of the method invocation.
     *
     * @return result of the invoked method
     */
    public String getResultOfInvocation() {
        return resultOfInvocation;
    }
}
