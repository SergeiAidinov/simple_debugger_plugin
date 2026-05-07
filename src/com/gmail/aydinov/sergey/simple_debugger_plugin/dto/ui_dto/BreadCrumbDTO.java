package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.Optional;

public class BreadCrumbDTO {
	
	private final int breadCrumbOrder;
	private final String description;
	private final Optional<Integer> pageNumber;
	private final boolean holdsHead;
	
	public BreadCrumbDTO(int breadCrumbOrder, String description, Integer pageNumber,  boolean holdsHead) {
		super();
		this.breadCrumbOrder = breadCrumbOrder;
		this.description = description;
		this.pageNumber = Optional.ofNullable(pageNumber);
		this.holdsHead = holdsHead;
	}

	public boolean doesHoldHead() {
		return holdsHead;
	}

	public int getBreadCrumbOrder() {
		return breadCrumbOrder;
	}
	
	public Optional<Integer> getPageNumber() {
		return pageNumber;
	}

	public String getDescription() {
		return description;
	}

}
