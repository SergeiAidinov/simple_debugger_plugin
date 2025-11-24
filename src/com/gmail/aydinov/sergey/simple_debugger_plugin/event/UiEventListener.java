package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.events.UIEvent;

/**
 * слушатель событий UI → дебаггер
 */
public interface UiEventListener {

    void handleUiEvent(UIEvent uIevent);
}

