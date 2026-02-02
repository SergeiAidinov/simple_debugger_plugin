package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;

/**
 * Singleton, управляющий окном отладчика.
 * Позволяет получить текущее окно или создать новое при необходимости.
 */
public class DebugWindowManager {

    private static DebugWindowManager INSTANCE;

    private DebugWindow debugWindow;

    private DebugWindowManager() {
        // private constructor for singleton
    }

    /**
     * Возвращает единственный экземпляр DebugWindowManager.
     */
    public static synchronized DebugWindowManager instance() {
        if (INSTANCE == null) {
            INSTANCE = new DebugWindowManager();
        }
        return INSTANCE;
    }

    /**
     * Возвращает текущее окно отладчика, создавая его, если оно не существует или закрыто.
     * @return объект DebugWindow или null, если отладка не запущена.
     */
    public DebugWindow getOrCreateWindow() {
        if (!DebuggerContext.context().isRunning()) {
            return null;
        }

        if (debugWindow == null || !debugWindow.isOpen()) {
            debugWindow = new DebugWindow();

            // Открываем окно в UI-потоке
            Display.getDefault().asyncExec(() -> debugWindow.open());
        }

        return debugWindow;
    }
}
