package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;

public class BreadcrumbItemDTO {
	
	    private final String displayName;
	    private final UniversalElementType elementType;
	    private final ValueCategory valueCategory;
	    private final boolean canInspect;
	    private final Tag tag;

	    public BreadcrumbItemDTO(String displayName, UniversalElementType elementType, ValueCategory valueCategory, boolean canInspect, Tag tag) {
	        this.displayName = displayName;
	        this.elementType = elementType;
	        this.valueCategory = valueCategory;
	        this.canInspect = canInspect;
	        this.tag = tag;
	    }

	    public String getDisplayName() { return displayName; }
	    public UniversalElementType getElementType() { return elementType; }
	    public ValueCategory getValueCategory() { return valueCategory; }
		public boolean isCanInspect() { return canInspect; }
		public Tag getTag() { return tag; }
}
