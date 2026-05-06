package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeanceCache;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils.UiUtils;

public class InspectionSeanceCacheImpl implements InspectionSeanceCache {

	private final Map<Long, DataProviderHolderImpl> dataProviderHolders = new ConcurrentHashMap<Long, DataProviderHolderImpl>();
	private final SortedMap<Integer, BreadCrumb> breadCrumbs = new java.util.concurrent.ConcurrentSkipListMap<>();
	private final AtomicInteger breadCrumbOrder = new AtomicInteger(0);
	private final AtomicInteger headPosition = new AtomicInteger(0);

	@Override
	public Map<Long, DataProviderHolderImpl> getDataProviderHolders() {
		return dataProviderHolders;
	}

	@Override
	public SortedMap<Integer, BreadCrumb> getBreadcrumbs() {
		return breadCrumbs;
	}

	@Override
	public void addBreadCrumbIfNecessary(Long objectId, AbstractUIEvent abstractUIEvent, String description) {
		int order = -1;
		if (abstractUIEvent instanceof UIEvent uiEvent) {
			if (uiEvent.getPayload() instanceof PairDTO pair) {
				if (pair.getSecond() instanceof Integer cuurrentBreadCrumbOrder) {
					order = cuurrentBreadCrumbOrder;
				}
			}
		}
		BreadCrumb existingBreadCrumb = breadCrumbs.get(order);
		if (Objects.nonNull(existingBreadCrumb)) {
			headPosition.set(order);
		} else {
			order = breadCrumbOrder.getAndIncrement();
			BreadCrumb breadCrumb = new BreadCrumb(order, objectId, abstractUIEvent, description, false);
			breadCrumbs.put(order, breadCrumb);
			headPosition.set(order);
		}

	}

	@Override
	public List<PairDTO<Integer, BreadCrumb>> groupBreadCrumbsintoPairs() {
		List<PairDTO<Integer, BreadCrumb>> result = new ArrayList<PairDTO<Integer, BreadCrumb>>();
		for (Entry<Integer, BreadCrumb> orderAndBreadCrumb : breadCrumbs.entrySet()) {
			final int order = orderAndBreadCrumb.getKey();
			final int currentHeadPosition = headPosition.get();
			orderAndBreadCrumb.getValue().setHoldsHead(order == currentHeadPosition);
			result.add(PairDTO.of(order, orderAndBreadCrumb.getValue()));
		}
		return result;
	}

	@Override
	public void setHeadPosition(int order) {
		headPosition.set(order);
	}
}
