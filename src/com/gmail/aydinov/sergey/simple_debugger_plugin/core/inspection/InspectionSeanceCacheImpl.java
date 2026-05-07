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
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.BreadCrumbDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

public class InspectionSeanceCacheImpl implements InspectionSeanceCache {

	private final Map<Long, DataProviderHolderImpl> dataProviderHolders = new ConcurrentHashMap<Long, DataProviderHolderImpl>();
	private final SortedMap<Integer, NavigationHistoryStep> breadCrumbs = new java.util.concurrent.ConcurrentSkipListMap<>();
	private final AtomicInteger breadCrumbOrder = new AtomicInteger(0);
	private final AtomicInteger headPosition = new AtomicInteger(0);

	@Override
	public Map<Long, DataProviderHolderImpl> getDataProviderHolders() {
		return dataProviderHolders;
	}

	@Override
	public SortedMap<Integer, NavigationHistoryStep> getBreadcrumbs() {
		return breadCrumbs;
	}

	@Override
	public void addBreadCrumbIfNecessary(Long objectId, AbstractUIEvent abstractUIEvent, String description, Integer pageNumber) {
//		Integer order = -1;
//		if (abstractUIEvent instanceof UIEvent uiEvent) {
//			if (uiEvent.getPayload() instanceof PairDTO pair) {
//				if (pair.getSecond() instanceof Integer cuurrentBreadCrumbOrder) {
//					order = cuurrentBreadCrumbOrder;
//				}
//			}
//		}
		NavigationHistoryStep desiredNavigationHistoryStep = null;
		System.out.println("REQUESTED PAGE #: " + pageNumber);
		breadCrumbs.entrySet().stream().forEach(e -> System.out.println(e));
		for (Entry<Integer, NavigationHistoryStep> entry : breadCrumbs.entrySet()) {
			if (Objects.equals(entry.getValue().getPageNumber().get(), pageNumber)) {
				desiredNavigationHistoryStep = breadCrumbs.get(entry.getKey());
				break;
			}
		}
		// NavigationHistoryStep existingBreadCrumb = breadCrumbs.get(order);
		if (Objects.nonNull(desiredNavigationHistoryStep)) {
			headPosition.set(desiredNavigationHistoryStep.getBreadCrumbOrder());
		} else {
			int order = breadCrumbOrder.getAndIncrement();
			NavigationHistoryStep breadCrumb = new NavigationHistoryStep(order, objectId, abstractUIEvent, description,
					false, pageNumber);
			breadCrumbs.put(order, breadCrumb);
			headPosition.set(order);
		}

	}

	@Override
	public List<PairDTO<Integer, BreadCrumbDTO>> groupBreadCrumbsintoPairs() {
		List<PairDTO<Integer, BreadCrumbDTO>> result = new ArrayList<PairDTO<Integer, BreadCrumbDTO>>();
		for (Entry<Integer, NavigationHistoryStep> orderAndBreadCrumb : breadCrumbs.entrySet()) {
			final int order = orderAndBreadCrumb.getKey();
			final int currentHeadPosition = headPosition.get();
			orderAndBreadCrumb.getValue().setHoldsHead(order == currentHeadPosition);
			result.add(PairDTO.of(order,
					new BreadCrumbDTO(order, orderAndBreadCrumb.getValue().getDescription(),
							orderAndBreadCrumb.getValue().getPageNumber().get(),
							orderAndBreadCrumb.getValue().doesHoldHead())));
		}
		return result;
	}

	@Override
	public void setHeadPosition(int order) {
		headPosition.set(order);
	}
}
