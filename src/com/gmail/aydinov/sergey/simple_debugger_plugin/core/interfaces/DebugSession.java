package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.sun.jdi.event.BreakpointEvent;

public interface DebugSession extends Runnable {
//
//    /** Запускает обработку событий (блокирующий вызов) */
//    void runLoop();

    /** Запрашивает остановку цикла (не обязательно мгновенную) */
    void stop();
    
    boolean refreshUI(BreakpointEvent breakpointEvent);
}
