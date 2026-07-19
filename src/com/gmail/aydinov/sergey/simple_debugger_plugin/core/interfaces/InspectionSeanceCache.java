package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.NavigationHistoryStep;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.PageableDataProviderHolderImpl;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.holder.DataProviderHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.BreadCrumbDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.sun.jdi.ObjectReference;

public interface InspectionSeanceCache {
	
	Map<Long, DataProviderHolder> getDataProviderHolders();
	SortedMap<Integer, NavigationHistoryStep> getBreadcrumbs();
	void addOrModifyBreadCrumb(Long objectId, AbstractUIEvent abstractUIEvent, String description,  Integer pageNumber);
	List<PairDTO<Integer, BreadCrumbDTO>> groupBreadCrumbsintoPairs();
	void setHeadPosition(int position);
	Map<Long, ObjectReference> getLoadedPieces();
}
