package com.gmail.aydinov.sergey.simple_debugger_plugin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.gmail.aydinov.sergey.simple_debugger_plugin.wrapper.TargetAppWrapper;

public class SimpleDebugPluginStarter extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            System.out.println("Launching TargetAppWrapper from plugin...");

            // Здесь можно передать имя таргет-класса как аргумент
            String targetClass = "target_debug.Main";
            TargetAppWrapper.main(new String[]{targetClass});

            System.out.println("TargetAppWrapper finished execution.");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
