package com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractSimpleDebuggerEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;

public abstract class AbstractUIEvent extends AbstractSimpleDebuggerEvent {
    protected AbstractUIEvent(SimpleDebuggerEventTypes.SimpleDebuggerEventType type) {
        super(type);
    }
}
