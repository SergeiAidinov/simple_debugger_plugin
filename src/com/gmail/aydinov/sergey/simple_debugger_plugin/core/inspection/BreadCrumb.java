package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

public class BreadCrumb {
	
	private final int breadCrumbOrder;
	private final long objectId;
	private final AbstractUIEvent abstractUIEvent;
	private final String description;
	private boolean holdsHead;
	
	public BreadCrumb(int breadCrumbOrder, long objectId, AbstractUIEvent abstractUIEvent, String description, boolean holdsHead) {
		this.breadCrumbOrder = breadCrumbOrder;
		this.objectId = objectId;
		this.abstractUIEvent = abstractUIEvent;
		this.description = description;
		this.holdsHead = holdsHead;
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
	
}
