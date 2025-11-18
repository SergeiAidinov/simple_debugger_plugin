package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import org.eclipse.swt.widgets.Display;
import com.sun.jdi.Location;

public class DebugWindowManager {

    private static DebugWindow window;

    // Открывает окно, если оно ещё не открыто, и делает активным
    public static void openAndFocus() {
        Display.getDefault().asyncExec(() -> {
            if (window == null || window.getShell().isDisposed()) {
                window = new DebugWindow(Display.getDefault());
                window.open();
            } else {
                window.getShell().setActive();
            }
        });
    }

    // Передаёт новые данные (Location) в окно
    public static void update(Location location) {
        Display.getDefault().asyncExec(() -> {
            if (window != null && !window.getShell().isDisposed()) {
                window.updateLocation(location);
            }
        });
    }
    
    public void updateLocation(Location location) {
        // Проверяем, что окно создано и не закрыто
        if (window != null && !window.getShell().isDisposed()) {
            // Обновляем данные во всех вкладках
            window.updateLocation(location);
        } else {
            // Если окно ещё не создано — создаём его
            Display.getDefault().asyncExec(() -> {
                if (window == null || window.getShell().isDisposed()) {
                    window = new DebugWindow(Display.getDefault());
                    window.open();
                }
                window.updateLocation(location);
            });
        }
    }

}
