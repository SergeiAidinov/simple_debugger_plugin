package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.sun.jdi.Location;
import com.sun.jdi.ThreadReference;

/**
Слушатель событий от дебаггера → UI
**/

public interface DebugEventListener {

    /** Остановились на брейкпойнте */
    void onBreakpointHit(Location location, ThreadReference thread);

    /** Виртуальная машина возобновила выполнение */
    void onResume();

    /** Дебаггер завершился */
    void onTerminate();
}
