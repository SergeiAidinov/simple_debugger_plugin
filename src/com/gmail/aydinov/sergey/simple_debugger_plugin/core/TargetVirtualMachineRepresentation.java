package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;

public class TargetVirtualMachineRepresentation {
	private static TargetVirtualMachineRepresentation instance;
	private static VirtualMachine virtualMachine = null;
	private final String host;
	private final Integer port;

	private TargetVirtualMachineRepresentation(String host, Integer port) {
		this.host = host;
		this.port = port;
	}

	public static synchronized TargetVirtualMachineRepresentation instance(String host, Integer port) {
		if (Objects.isNull(instance)) {
			instance = new TargetVirtualMachineRepresentation(host, port);
			configureVirtualMachine(host, port);
		} else if (!instance.getHost().equals(host) || !instance.getPort().equals(port)) {
			throw new IllegalStateException("ConfigManager already initialized with a different path!");
		}
		return instance;
	}

	public VirtualMachine getVirtualMachine() {
		return virtualMachine;
	}

	public String getHost() {
		return host;
	}

	public Integer getPort() {
		return port;
	}

	private static boolean configureVirtualMachine(String host, Integer port) {
		VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
		AttachingConnector connector = virtualMachineManager.attachingConnectors().stream()
				.filter(c -> c.name().equals("com.sun.jdi.SocketAttach")).findAny().orElseThrow();
		Map<String, Connector.Argument> arguments = connector.defaultArguments();
		arguments.get("hostname").setValue(host);
		arguments.get("port").setValue(String.valueOf(port));
		VirtualMachine vm = null;
		while (true) {
			System.out.println("Connecting to " + host + ":" + port + "...");
			try {
				vm = connector.attach(arguments);
			} catch (Exception e) {
				try {
					TimeUnit.SECONDS.sleep(2);
				} catch (InterruptedException interruptedException) {
					continue;
				}
				continue;
			}
			if (Objects.nonNull(vm)) {
				virtualMachine = vm;
				System.out.println("Successfully connected to target Virtual Machine at " + host + ":" + port + ".");
				break;
			}
		}
		return true;
	}

}