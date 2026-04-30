package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

public class BreadCrumb {
	
	private final int breadCrumbOrder;
	private final long objectId;
	private final AbstractUIEvent abstractUIEvent;
	private final String description;
	
	public BreadCrumb(int breadCrumbOrder, long objectId, AbstractUIEvent abstractUIEvent, String description) {
		this.breadCrumbOrder = breadCrumbOrder;
		this.objectId = objectId;
		this.abstractUIEvent = abstractUIEvent;
		this.description = description;
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
}
