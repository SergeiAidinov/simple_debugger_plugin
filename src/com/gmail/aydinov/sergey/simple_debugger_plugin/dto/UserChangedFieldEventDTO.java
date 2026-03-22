package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;

public class UserChangedFieldEventDTO {

    private final Tag tag;
    private final String newValue;

    public UserChangedFieldEventDTO(Tag tag, Object newValue) {
        this.tag = tag;
        this.newValue = Objects.nonNull(newValue) ? newValue.toString() : null;
    }
    
    public Tag getTag() {
		return tag;
	}

	public String getNewValue() {
        return newValue;
    }
    
	@Override
	public String toString() {
		return "UserChangedFieldEventDTO [tag=" + tag + ", newValue=" + newValue + "]";
	}
    
}
