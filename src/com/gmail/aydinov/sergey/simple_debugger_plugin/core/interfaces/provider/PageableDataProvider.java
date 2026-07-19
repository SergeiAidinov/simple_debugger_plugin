package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider;

public interface PageableDataProvider extends DataProvider {
	
	void handlePageRequest(Integer pageNumber);

}
