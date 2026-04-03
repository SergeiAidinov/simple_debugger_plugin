package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;

/**
 * Represents an inspectable element that can be converted into a DTO representation.
 *
 * @param <T> the type of DTO returned by the inspection, must extend AbstractInspectionDTO
 */
public interface Inspectable<T extends AbstractInspectionDTO> {
    
    /**
     * Inspect the given element and return a DTO representation of it.
     *
     * @param inspectableElement the element to inspect
     * @return a DTO containing the inspection result
     */
    T inspect(AbstractInspectableElement inspectableElement);
}