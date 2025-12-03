package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugEventProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.TargetApplicationStatusProvider;

public class DebugWindowManager {
	
    private static DebugWindowManager INSTANCE;
    private DebugWindow debugWindow;
    private DebugEventProvider debugEventProvider;
    private TargetApplicationStatusProvider targetApplicationStatusProvider;

    private DebugWindowManager() {
    }

    public static synchronized DebugWindowManager instance() {
        if (INSTANCE == null) {
            INSTANCE = new DebugWindowManager();
        }
        return INSTANCE;
    }
    
	public void setTargetApplicationStatusProvider(TargetApplicationStatusProvider targetApplicationStatusProvider) {
		this.targetApplicationStatusProvider = targetApplicationStatusProvider;
	}

	/** Возвращает текущее окно или создаёт его, если его нет */
    public DebugWindow getOrCreateWindow() {
    	 TargetApplicationStatus qq = targetApplicationStatusProvider.getTargetApplicationStatus();
    	 System.out.println(qq);
    	if (targetApplicationStatusProvider.getTargetApplicationStatus().equals(TargetApplicationStatus.STOPPING)) return null;
        if (debugWindow == null || !debugWindow.isOpen()) {
            debugWindow = new DebugWindow();
            Display.getDefault().asyncExec(() -> {
                debugWindow.open(); // открываем shell в UI-потоке
            });
        }
        debugWindow.setDebugEventProvider(debugEventProvider);
        return debugWindow;
    }
    
}
