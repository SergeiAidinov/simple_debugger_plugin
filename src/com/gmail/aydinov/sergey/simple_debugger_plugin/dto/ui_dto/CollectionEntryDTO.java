package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

public class CollectionEntryDTO {

    private final int index;
    private final Object key;
    private final Object value;

    public CollectionEntryDTO(int index, Object key, Object value) {
        this.index = index;
        this.key = key;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}
