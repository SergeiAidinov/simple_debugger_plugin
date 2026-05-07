package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import java.util.Objects;
import java.util.Optional;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

public class NavigationHistoryStep {
	
	private final int breadCrumbOrder;
	private final long objectId;
	private final AbstractUIEvent abstractUIEvent;
	private final String description;
	private boolean holdsHead;
	private final Optional<Integer> pageNumber;
	
	public NavigationHistoryStep(int breadCrumbOrder, long objectId, AbstractUIEvent abstractUIEvent, String description, boolean holdsHead, Integer pageNumber) {
		this.breadCrumbOrder = breadCrumbOrder;
		this.objectId = objectId;
		this.abstractUIEvent = abstractUIEvent;
		this.description = description;
		this.holdsHead = holdsHead;
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

	public boolean doesHoldHead() {
		return holdsHead;
	}
	
	public void setHoldsHead(boolean helds) {
		holdsHead = helds;
	}
	
	public Optional<Integer> getPageNumber() {
		return pageNumber;
	}

	@Override
	public String toString() {
		return "NavigationHistoryStep [breadCrumbOrder=" + breadCrumbOrder + ", objectId=" + objectId
				+ ", abstractUIEvent=" + abstractUIEvent + ", description=" + description + ", holdsHead=" + holdsHead
				+ ", pageNumber=" + pageNumber + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(breadCrumbOrder, description, holdsHead, objectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NavigationHistoryStep other = (NavigationHistoryStep) obj;
		return breadCrumbOrder == other.breadCrumbOrder && Objects.equals(description, other.description)
				&& holdsHead == other.holdsHead && objectId == other.objectId;
	}
}
