package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.UIEvent;

/**
 * слушатель событий UI → дебаггер
 */
public interface UiEventListener {

    void handleUiEvent(UIEvent uIevent);
}

