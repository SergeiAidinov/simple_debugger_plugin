package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.BreadCrumb;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.DataProviderHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

public interface InspectionSeanceCache {
	
	Map<Long, DataProviderHolder> getDataProviderHolders();
	SortedMap<Integer, BreadCrumb> getBreadcrumbs();
	void addBreadCrumbIfNecessary(Long objectId, AbstractUIEvent abstractUIEvent, String description);
	List<PairDTO<Integer, String>> groupBreadCrumbsintoPairs();
	
	
	

}
