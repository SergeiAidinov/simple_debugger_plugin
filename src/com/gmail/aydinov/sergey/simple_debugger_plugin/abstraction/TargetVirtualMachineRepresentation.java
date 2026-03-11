package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import com.sun.jdi.VirtualMachine;
import java.util.Objects;

/**
 * Singleton representation of a target virtual machine.
 * Экземпляр создаётся один раз и больше не может быть пересоздан.
 */
public class TargetVirtualMachineRepresentation {

    private static TargetVirtualMachineRepresentation instance;

    private final String host;
    private final Integer port;
    private final VirtualMachine virtualMachine;

    /** Приватный конструктор — создаётся только через getInstanceFor(...) */
    private TargetVirtualMachineRepresentation(String host, Integer port, VirtualMachine virtualMachine) {
        Objects.requireNonNull(virtualMachine, "VirtualMachine не может быть null");
        this.host = host;
        this.port = port;
        this.virtualMachine = virtualMachine;
    }

    /**
     * Создаёт и возвращает экземпляр с параметрами.
     * Если экземпляр уже существует — бросает исключение.
     */
    public static synchronized TargetVirtualMachineRepresentation getInstanceFor(String host, Integer port, VirtualMachine virtualMachine) {
        if (instance != null) {
            throw new IllegalStateException("TargetVirtualMachineRepresentation уже создан!");
        }
        instance = new TargetVirtualMachineRepresentation(host, port, virtualMachine);
        return instance;
    }

    /**
     * Возвращает уже созданный экземпляр.
     * Если экземпляр ещё не создан — бросает исключение.
     */
    public static TargetVirtualMachineRepresentation getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TargetVirtualMachineRepresentation ещё не создан. Вызовите getInstanceFor(...) первым!");
        }
        return instance;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }
}