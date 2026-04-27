package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import java.util.List;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public abstract class AbstractInspectionCollectionPage<T> extends AbstractInspectionDTO {

	private final int totalElements;
	private final int currentPage;
	private final int totalPages;
	private final List<T> entries;

	protected AbstractInspectionCollectionPage(Tag tag, String elementName, String elementType, int totalElements,
			int currentPage, int totalPages, List<T> entries, List<PairDTO<Integer, String>> breadcrumbs, Long objectId) {
		super(tag, elementName, elementType, breadcrumbs, objectId);
		this.totalElements = totalElements;
		this.currentPage = currentPage;
		this.totalPages = totalPages;
		this.entries = List.copyOf(entries);
	}


	public int getTotalElements() {
		return totalElements;
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public List<T> getEntries() {
		return entries;
	}

	public boolean hasNextPage() {
		return currentPage < totalPages - 1;
	}

	public boolean hasPreviousPage() {
		return currentPage > 0;
	}
}