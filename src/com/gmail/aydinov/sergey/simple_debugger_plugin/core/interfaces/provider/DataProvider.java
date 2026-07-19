package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider;

import java.util.concurrent.locks.ReentrantLock;

public interface DataProvider {

	// void setDataProviderHolderId(long id);

	long getDataProviderHolderId();

	public static ReentrantLock jdiAccessLock = new ReentrantLock(true);

}
