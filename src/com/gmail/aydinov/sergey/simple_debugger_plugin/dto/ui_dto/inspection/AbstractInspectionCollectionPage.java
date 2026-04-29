package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import java.util.List;
import java.util.Map;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public abstract class AbstractInspectionCollectionPage<T> extends AbstractInspectionDTO {

	private final String totalElements;
	private final int currentPage;
	private final String totalPages;
//	private final Map<Integer, UniversalElementRepresentation> entries;

	protected AbstractInspectionCollectionPage(Tag tag, String elementName, String elementType, String totalElements,
			int currentPage, String totalPages,  List<PairDTO<Integer, String>> breadcrumbs, Long objectId) {
		super(tag, elementName, elementType, breadcrumbs, objectId);
		this.totalElements = totalElements;
		this.currentPage = currentPage;
		this.totalPages = totalPages;
		
	}


	public String getTotalElements() {
		return totalElements;
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public String getTotalPages() {
		return totalPages;
	}

//	public Map<Integer, UniversalElementRepresentation> getEntries() {
//		return entries;
//	}

	public boolean hasNextPage() {
		return currentPage < Integer.valueOf(totalPages) - 1;
	}

	public boolean hasPreviousPage() {
		return currentPage > 0;
	}
}