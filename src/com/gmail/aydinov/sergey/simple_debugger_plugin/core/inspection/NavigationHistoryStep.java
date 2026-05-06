package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import java.util.Objects;
import java.util.Optional;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

public class NavigationHistoryStep {

	private final int breadCrumbOrder;
	private final long objectId;
	private final AbstractUIEvent abstractUIEvent;
	private final String description;
	private final Optional<Integer> pageNumber;

	public NavigationHistoryStep(int breadCrumbOrder, long objectId, AbstractUIEvent abstractUIEvent,
			String description, Integer pageNumber) {
		this.breadCrumbOrder = breadCrumbOrder;
		this.objectId = objectId;
		this.abstractUIEvent = abstractUIEvent;
		this.description = description;
		this.pageNumber = Optional.ofNullable(pageNumber);
	}

	public int getBreadCrumbOrder() {
		return breadCrumbOrder;
	}

	public long getObjectId() {
		return objectId;
	}

	public AbstractUIEvent getAbstractUIEvent() {
		return abstractUIEvent;
	}

	public String getDescription() {
		return description;
	}

	public Optional<Integer> getPageNumber() {
		return pageNumber;
	}
}
