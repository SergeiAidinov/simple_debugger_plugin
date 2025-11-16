package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequestManager;

public class TargetVirtualMachineRepresentation {
	
	private final String host;
	private final Integer port;
	private final VirtualMachine virtualMachine;
	
	public TargetVirtualMachineRepresentation(String host, Integer port, VirtualMachine virtualMachine) {
		super();
		this.host = host;
		this.port = port;
		this.virtualMachine = virtualMachine;
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
