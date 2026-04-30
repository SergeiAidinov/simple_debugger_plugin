package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

public class BreadCrumb {
	
	private final int BreadCrumbOrder;
	private final long objectId;
	private final AbstractUIEvent abstractUIEvent;
	
	public BreadCrumb(int breadCrumbOrder, long objectId, AbstractUIEvent abstractUIEvent) {
		BreadCrumbOrder = breadCrumbOrder;
		this.objectId = objectId;
		this.abstractUIEvent = abstractUIEvent;
	}

	public int getBreadCrumbOrder() {
		return BreadCrumbOrder;
	}

	public long getObjectId() {
		return objectId;
	}

	public AbstractUIEvent getAbstractUIEvent() {
		return abstractUIEvent;
	}
}
