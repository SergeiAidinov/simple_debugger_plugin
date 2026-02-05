package com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;

/**
 * Event representing an update to the debugger console output.
 */
public class ConsoleUpdateDebugEvent extends AbstractSimpleDebugEvent {

    /** Text to be displayed in the console */
    private final String text;

    /**
     * Creates a new console update event.
     *
     * @param type the type of debugger event (e.g., REFRESH_CONSOLE)
     * @param text the text content to update in the console
     */
    public ConsoleUpdateDebugEvent(SimpleDebuggerEventType type, String text) {
        super(type);
        this.text = text;
    }

    /**
     * Returns the text content of the console update.
     *
     * @return the console text
     */
    public String getText() {
        return text;
    }
}
