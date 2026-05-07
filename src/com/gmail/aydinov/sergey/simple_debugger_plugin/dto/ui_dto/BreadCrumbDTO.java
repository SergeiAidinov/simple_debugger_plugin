package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

public class BreadCrumbDTO {
	
	private final int breadCrumbOrder;
	private final String description;
	private final boolean holdsHead;
	
	public BreadCrumbDTO(int breadCrumbOrder, String description, boolean holdsHead) {
		super();
		this.breadCrumbOrder = breadCrumbOrder;
		this.description = description;
		this.holdsHead = holdsHead;
	}

	public boolean doesHoldHead() {
		return holdsHead;
	}

	public int getBreadCrumbOrder() {
		return breadCrumbOrder;
	}

	public String getDescription() {
		return description;
	}

}
