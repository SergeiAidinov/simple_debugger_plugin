package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.holder;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider.DataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider.PageableDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractSimpleDebuggerEvent;

public interface DataProviderHolder {

	void handleEvent(AbstractSimpleDebuggerEvent abstractSimpleDebuggerEvent);

	void startDataProvider();

	void terminateCurrentRequest();

	DataProvider getDataProvider();

}