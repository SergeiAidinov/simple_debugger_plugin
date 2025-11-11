package com.example.app.lifecycle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.ui.IStartup;

import jakarta.annotation.PreDestroy;


/**
 * Класс-менеджер жизненного цикла.
 * Методы будут вызываться автоматически системой e4.
 */
public class AppLifeCycle implements IStartup{

    /**
     * Вызывается на самом раннем этапе, когда создан контекст приложения (аналог start() Activator'а).
     */
	@org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate
    void handlePostContextCreate(IEclipseContext context) {
    	System.out.println("+++++++++++++++++++++++++++++++++");
    	Platform.getLog(getClass()).log(new Status(
                IStatus.INFO, 
                "com.example.app.lifecycle", // Ваш Bundle-SymbolicName
                "✅ LifeCycle Manager: @PostContextCreate - Глобальный контекст создан."
        ));
        
        // Пример: регистрация глобального сервиса или настройки
        context.set("appName", "My e4 LifeCycle Demo");
    }

    /**
     * Вызывается, когда приложение закрывается.
     */
	
	  @PreDestroy void handlePreDestroy(IEclipseContext context) { 
		  String name = (String) context.get("appName");
	  System.out.println("❌ LifeCycle Manager: @PreDestroy - Приложение '" + name +
	  "' закрывается. Очистка..."); }

	  @Override
		public void earlyStartup() {
			 System.out.println("AppStartup сработал!");
		}
	 
}