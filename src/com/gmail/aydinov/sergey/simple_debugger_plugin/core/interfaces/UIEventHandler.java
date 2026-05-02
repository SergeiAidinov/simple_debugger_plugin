package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

@FunctionalInterface
public interface UIEventHandler {
    /**
     * Обрабатывает событие UI.
     *
     * @param event событие UI
     * @return true, если после обработки нужно обновить snapshot и UI
     */
    boolean handle(HandlerContext abstractUIEventContext, AbstractUIEvent abstractSimpleDebuggerUIEvent);
}
