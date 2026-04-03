package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
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
    private final List<BreadcrumbItemDTO> breadcrumbs; // breadcrumb path for UI

    /**
     * @param tag          unique tag of this element
     * @param elementName  display name of this element
     * @param elementType  type of this element
     * @param breadcrumbs  list of breadcrumb items representing the path from root
     */
    protected AbstractInspectionDTO(Tag tag, String elementName, String elementType, List<BreadcrumbItemDTO> breadcrumbs) {
        if (tag == null) throw new IllegalArgumentException("Tag must not be null");
        this.tag = tag;
        this.elementName = elementName;
        this.elementType = elementType;
        this.breadcrumbs = breadcrumbs != null ? List.copyOf(breadcrumbs) : Collections.emptyList();
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
    public List<BreadcrumbItemDTO> getBreadcrumbs() {
        return breadcrumbs;
    }
}