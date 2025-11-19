package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.swt.widgets.Display;

import com.sun.jdi.Location;
import com.sun.jdi.ThreadReference;

public class DebugWindowManager {

    private static DebugWindowManager INSTANCE;
    private DebugWindow window;

    private DebugWindowManager() {}

    public static synchronized DebugWindowManager instance() {
        if (INSTANCE == null) {
            INSTANCE = new DebugWindowManager();
        }
        return INSTANCE;
    }

    /** Возвращает текущее окно или создаёт его, если его нет */
    public DebugWindow getOrCreateWindow() {
        if (window == null || !window.isOpen()) {
            window = new DebugWindow();
            Display.getDefault().asyncExec(() -> {
                window.open(); // открываем shell в UI-потоке
            });
        }
        return window;
    }

    /** Обновляет окно данными из JDI */
    public void updateLocation(Location loc, ThreadReference thread) {
        Display.getDefault().asyncExec(() -> {
            DebugWindow w = getOrCreateWindow();
            if (w != null && w.isOpen()) {
                w.updateLocation(loc, thread);
            }
        });
    }
}
