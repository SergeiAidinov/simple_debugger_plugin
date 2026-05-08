package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.NavigationHistoryStep;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.BreadCrumbDTO;

import java.util.Collections;
import java.util.List;

/**
 * Base class for any inspection DTO.
 * Holds element metadata and breadcrumb items for UI navigation.
 */
public abstract class AbstractInspectionDTO {

    private final Tag tag;
    private final String elementName;
    private final String elementType;
    private List<PairDTO<Integer, BreadCrumbDTO>> breadcrumbs; // breadcrumb path for UI
    private final Long objectId;

    /**
     * @param tag          unique tag of this element
     * @param elementName  display name of this element
     * @param elementType  type of this element
     * @param breadcrumbs2  list of breadcrumb items representing the path from root
     */
    protected AbstractInspectionDTO(Tag tag, String elementName, String elementType, List<PairDTO<Integer, BreadCrumbDTO>> breadcrumbs, Long objectId) {
      //  if (tag == null) throw new IllegalArgumentException("Tag must not be null");
        this.tag = tag;
        this.elementName = elementName;
        this.elementType = elementType;
        this.breadcrumbs = breadcrumbs;
        this.objectId = objectId;
    }

    public Tag getTag() {
        return tag;
    }

    public String getElementName() {
        return elementName;
    }

    public String getElementType() {
        return elementType;
    }

    /**
     * Returns the breadcrumb path from root to this element for UI display.
     */
    public List<PairDTO<Integer, BreadCrumbDTO>> getBreadcrumbs() {
        return breadcrumbs;
    }

	public void setBreadcrumbs(List<PairDTO<Integer, BreadCrumbDTO>> breadCrumbs) {
		this.breadcrumbs = breadCrumbs;
	}

	public Long getObjectId() {
		return objectId;
	}
	
}