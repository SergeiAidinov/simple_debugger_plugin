package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeanceCache;

public class InspectionSeanceCacheImpl implements InspectionSeanceCache{
	
	private final Map<Long, DataProviderHolder> dataProviderHolders = new ConcurrentHashMap<Long, DataProviderHolder>();

	@Override
	public Map<Long, DataProviderHolder> getDataProviderHolders() {
		return dataProviderHolders;
	}

}
