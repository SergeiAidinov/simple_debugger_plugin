package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.DataProviderHolder;

public interface InspectionSeanceCache {
	
	Map<Long, DataProviderHolder> getDataProviderHolders();
	
	
	

}
