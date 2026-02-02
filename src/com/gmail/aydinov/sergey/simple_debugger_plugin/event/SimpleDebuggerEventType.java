package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

/**
 * Types of events emitted by the simple debugger.
 */
public enum SimpleDebuggerEventType {

    /** Event triggered when the debugger stops at a breakpoint */
    STOPPED_AT_BREAKPOINT,

    /** Event triggered to refresh the debugger console */
    REFRESH_CONSOLE,

    /** Event triggered when a method is invoked in the target application */
    METHOD_INVOKE

}
