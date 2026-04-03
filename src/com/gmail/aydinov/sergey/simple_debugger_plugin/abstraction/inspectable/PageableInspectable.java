package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionCollectionPage;

/**
 * Represents an inspectable collection that supports pagination.
 *
 * @param <T> the type of DTO representing a page of the collection,
 *           must extend AbstractInspectionCollectionPage with any element type
 */
public interface PageableInspectable<T extends AbstractInspectionCollectionPage<?>> {

    /**
     * Retrieve a specific page of the inspected collection.
     *
     * @param inspectableElement the collection element to inspect
     * @param pageNumber zero-based page number to retrieve
     * @return a DTO representing the requested page of the collection
     */
    T inspectPage(AbstractInspectableElement inspectableElement, int pageNumber);
}