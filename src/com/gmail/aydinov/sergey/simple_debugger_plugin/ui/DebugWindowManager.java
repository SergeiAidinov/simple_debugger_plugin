package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugEventProvider;

public class DebugWindowManager {
	
    private static DebugWindowManager INSTANCE;
    private DebugWindow debugWindow;
    private DebugEventProvider debugEventProvider;

    private DebugWindowManager() {
    }

    public static synchronized DebugWindowManager instance() {
        if (INSTANCE == null) {
            INSTANCE = new DebugWindowManager();
        }
        return INSTANCE;
    }
    

	/** Возвращает текущее окно или создаёт его, если его нет */
    public DebugWindow getOrCreateWindow() {
//    	 TargetApplicationStatus qq = targetApplicationStatusProvider.getTargetApplicationStatus();
//    	 System.out.println(qq);
    	if (!DebuggerContext.context().isRunning()) return null;
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
