package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

public interface PageableDataProvider extends DataProvider {
	
	void handlePageRequest(Integer pageNumber);

}
