package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;

public class BreadcrumbItemDTO {
	
	    private final String displayName;
	    private final String iconKey;
	    private final UniversalElementType elementType;
	    private final boolean canInspect;

	    public BreadcrumbItemDTO(String displayName, String iconKey, UniversalElementType elementType, boolean canInspect) {
	        this.displayName = displayName;
	        this.iconKey = iconKey;
	        this.elementType = elementType;
	        this.canInspect = canInspect;
	    }

	    public String getDisplayName() { return displayName; }
	    public String getIconKey() { return iconKey; }
	    public UniversalElementType getElementType() { return elementType; }
	    public boolean isCanInspect() { return canInspect; }
	

}
