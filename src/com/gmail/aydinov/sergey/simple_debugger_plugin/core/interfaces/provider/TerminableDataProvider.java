package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider;

public interface TerminableDataProvider extends PageableDataProvider {
	
	void terminateCurrentRequest();

}
