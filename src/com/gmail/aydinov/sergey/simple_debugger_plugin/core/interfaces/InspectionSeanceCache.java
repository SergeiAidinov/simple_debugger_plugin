package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.NavigationHistoryStep;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.DataProviderHolderImpl;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.BreadCrumbDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

public interface InspectionSeanceCache {
	
	Map<Long, DataProviderHolderImpl> getDataProviderHolders();
	SortedMap<Integer, NavigationHistoryStep> getBreadcrumbs();
	void addBreadCrumbIfNecessary(Long objectId, AbstractUIEvent abstractUIEvent, String description);
	List<PairDTO<Integer, BreadCrumbDTO>> groupBreadCrumbsintoPairs();
	void setHeadPosition(int position);
}
