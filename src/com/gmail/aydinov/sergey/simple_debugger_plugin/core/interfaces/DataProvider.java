package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import java.util.concurrent.locks.ReentrantLock;

public interface DataProvider {
	
	void requestPage(Integer pageNumber);
	
	public static ReentrantLock jdiAccessLock = new ReentrantLock(true);

}
