package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeanceCache;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

public class InspectionSeanceCacheImpl implements InspectionSeanceCache{
	
	private final Map<Long, DataProviderHolder> dataProviderHolders = new ConcurrentHashMap<Long, DataProviderHolder>();
	private final SortedMap<Integer, BreadCrumb> breadCrumbs = new java.util.concurrent.ConcurrentSkipListMap<>();
	private final AtomicInteger breadCrumbOrder = new AtomicInteger(0);

	@Override
	public Map<Long, DataProviderHolder> getDataProviderHolders() {
		return dataProviderHolders;
	}

	@Override
	public SortedMap<Integer, BreadCrumb> getBreadcrumbs() {
		return breadCrumbs;
	}
	
	@Override
	public void addBreadCrumbIfNecessary(Long objectId, AbstractUIEvent abstractUIEvent, String description) {
		final int order = breadCrumbOrder.getAndIncrement();
		BreadCrumb breadCrumb = new BreadCrumb(order, objectId, abstractUIEvent, description);
		breadCrumbs.put(order, breadCrumb);

	}
	
	@Override
	public List<PairDTO<Integer, String>> groupBreadCrumbsintoPairs() {
		List<PairDTO<Integer, String>> result = new ArrayList<PairDTO<Integer, String>>();
		for (BreadCrumb breadCrumb : breadCrumbs.values()) {
			result.add(PairDTO.of(breadCrumb.getBreadCrumbOrder(), breadCrumb.getDescription()));
		}
		return result;
	}

}
