package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugEvent;

/**
Слушатель событий от дебаггера → UI
**/

public interface DebugEventListener {

   void handleDebugEvent(DebugEvent debugEvent);
}
