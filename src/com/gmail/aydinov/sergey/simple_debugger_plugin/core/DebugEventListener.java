package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.SimpleDebugEvent;

/**
Слушатель событий от дебаггера → UI
*/

public interface DebugEventListener {

   void handleDebugEvent(SimpleDebugEvent debugEvent);
}
