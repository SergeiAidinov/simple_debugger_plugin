package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;

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

    public static VirtualMachine launch(String mainClass, List<String> options) {
        try {
            LaunchingConnector connector = Bootstrap.virtualMachineManager()
                    .defaultConnector(); // default: LaunchingConnector
            Map<String, Connector.Argument> args = connector.defaultArguments();
            args.get("main").setValue(mainClass);
            if (options != null && !options.isEmpty()) {
                args.get("options").setValue(String.join(" ", options));
            }
            VirtualMachine vm = connector.launch(args);
            vm.suspend(); // сразу при старте останавливаем все потоки
            return vm;
        } catch (Exception e) {
            throw new RuntimeException("Cannot launch VM", e);
        }
    }
}

