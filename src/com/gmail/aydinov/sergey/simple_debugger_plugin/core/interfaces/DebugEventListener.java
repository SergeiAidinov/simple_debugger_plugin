package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEvent;

/**
Слушатель событий от дебаггера → UI
*/

public interface DebugEventListener {

   void handleDebugEvent(SimpleDebugEvent debugEvent);
}
