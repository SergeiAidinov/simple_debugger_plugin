package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

/**
 * DTO для передачи изменений переменной из UI в отладчик
 */
public class UserChangedVariableDTO extends AbstractUIEvent{

    private final String name;      // имя переменной
    private final String type;      // тип переменной (для справки)
    private final Object newValue;  // новое значение, введённое пользователем

    public UserChangedVariableDTO(String name, String type, Object newValue) {
        this.name = name;
        this.type = type;
        this.newValue = newValue;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Object getNewValue() {
        return newValue;
    }
}

