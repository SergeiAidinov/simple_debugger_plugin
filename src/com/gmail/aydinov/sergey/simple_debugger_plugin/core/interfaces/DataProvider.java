package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import java.util.concurrent.locks.ReentrantLock;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;

public interface DataProvider {
	
	AbstractInspectionDTO getData(Integer pageNumber);
	
	public static ReentrantLock jdiAccessLock = new ReentrantLock(true);

}
