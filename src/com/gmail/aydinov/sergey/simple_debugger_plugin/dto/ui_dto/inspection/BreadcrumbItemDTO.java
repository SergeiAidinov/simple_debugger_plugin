package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;

public class BreadcrumbItemDTO {
	
	    private final String displayName;
	   // private final String iconKey;
	    private final UniversalElementType elementType;
	    private final ValueCategory valueCategory;
	    private final boolean canInspect;

	    public BreadcrumbItemDTO(String displayName, /*String iconKey, */ UniversalElementType elementType, ValueCategory valueCategory, boolean canInspect) {
	        this.displayName = displayName;
	      //  this.iconKey = iconKey;
	        this.elementType = elementType;
	        this.valueCategory = valueCategory;
	        this.canInspect = canInspect;
	    }

	    public String getDisplayName() { return displayName; }
	   // public String getIconKey() { return iconKey; }
	    public UniversalElementType getElementType() { return elementType; }
	    public ValueCategory getValueCategory() { return valueCategory; }

		public boolean isCanInspect() { return canInspect; }
	

}
