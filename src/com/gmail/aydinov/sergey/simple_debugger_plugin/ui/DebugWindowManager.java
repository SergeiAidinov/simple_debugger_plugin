package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebugEventListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebugEventProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.UiEventListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.UiEventProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UIEvent;
import com.sun.jdi.Location;
import com.sun.jdi.ThreadReference;

public class DebugWindowManager implements DebugEventListener, UiEventProvider {
	
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
        if (debugWindow == null || !debugWindow.isOpen()) {
            debugWindow = new DebugWindow();
            Display.getDefault().asyncExec(() -> {
                debugWindow.open(); // открываем shell в UI-потоке
            });
        }
        debugWindow.setDebugEventProvider(debugEventProvider);
        //SimpleDebuggerWorkFlow.Factory.getInstanceOfSimpleDebuggerWorkFlow().setDebugEventListener(debugWindow);
        return debugWindow;
    }

    /** Обновляет окно данными из JDI */
	/*
	 * public void updateLocation(Location loc, ThreadReference thread) {
	 * Display.getDefault().asyncExec(() -> { DebugWindow w = getOrCreateWindow();
	 * if (w != null && w.isOpen()) { w.updateLocation(loc, thread); } }); }
	 */

	public void setDebugEventProvider(DebugEventProvider debugEventProvider) {
		this.debugEventProvider = debugEventProvider;
	}

	@Override
	public void sendUiEvent(UIEvent uiEvent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleDebugEvent(DebugEvent debugEvent) {
		// TODO Auto-generated method stub
		
	}
    
}
