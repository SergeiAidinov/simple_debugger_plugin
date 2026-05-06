package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractSimpleDebuggerEvent;

public interface DataProviderHolder {

	void handleEvent(AbstractSimpleDebuggerEvent abstractSimpleDebuggerEvent);

	void start();

	void stop();

	DataProvider getDataProvider();

}