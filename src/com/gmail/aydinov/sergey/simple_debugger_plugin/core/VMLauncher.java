package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class VMLauncher {

    public static VirtualMachine attach(String host, int port) {
        try {
            VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
            AttachingConnector connector = vmm.attachingConnectors().stream()
                    .filter(c -> c.name().equals("com.sun.jdi.SocketAttach")).findAny().orElseThrow();

            Map<String, Connector.Argument> args = connector.defaultArguments();
            args.get("hostname").setValue(host);
            args.get("port").setValue(String.valueOf(port));

            VirtualMachine vm;
            while (true) {
                try {
                    vm = connector.attach(args);
                    vm.suspend(); // останавливаем сразу после attach
                    break;
                } catch (Exception ignored) {
                    Thread.sleep(500);
                }
            }
            return vm;
        } catch (Exception e) {
            throw new RuntimeException("Cannot attach VM", e);
        }
    }

    public static VirtualMachine launch() {
        try {
            // Получаем стандартный LaunchingConnector
            LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
            Map<String, Connector.Argument> args = connector.defaultArguments();

            // Полностью квалифицированное имя main-класса
            args.get("main").setValue("target_debug.Main");

            // Classpath к скомпилированным классам (не исходники)
            args.get("options").setValue("-cp /home/sergei/eclipse-commiters-workspace/target_debug/bin"); 

            // Не останавливать сразу JVM
            args.get("suspend").setValue("false");

            // Запускаем target JVM
            VirtualMachine vm = connector.launch(args);

            System.out.println("==> VM LAUNCHED: " + vm.description());

            // Логируем stdout и stderr target-приложения
            Process process = vm.process();

            new Thread(() -> {
                try (InputStream in = process.getInputStream()) {
                    in.transferTo(System.out);
                } catch (Exception ignored) {}
            }).start();

            new Thread(() -> {
                try (InputStream err = process.getErrorStream()) {
                    err.transferTo(System.err);
                } catch (Exception ignored) {}
            }).start();

            return vm;

        } catch (Exception e) {
            throw new RuntimeException("Cannot launch VM", e);
        }
    }
}

