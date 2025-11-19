package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UIEvent;

/**
 * слушатель событий UI → дебаггер
 */
public interface UiEventListener {

    void handleUiEvent(UIEvent uIevent);
}

