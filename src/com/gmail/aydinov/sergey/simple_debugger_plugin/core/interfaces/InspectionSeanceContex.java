package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import java.util.SortedMap;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.BreadCrumb;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.DataProviderHolder;

public interface InspectionSeanceContex {
	DataProviderHolder getDataProviderHolderByInspectableObjectId(Long objectId);
	
	void addDataProviderHolder(Long objectId, DataProviderHolder DataProviderHolder);
	
	void addBreadCrumb(BreadCrumb breadCrumb);
	
	SortedMap<Integer, BreadCrumb> getBreadCrumbs();
}
