package com.gmail.aydinov.sergey.simple_debugger_plugin.exception;

public class VirtualMachineConfigurationException extends RuntimeException {
	
	private final String message;

	public VirtualMachineConfigurationException(String message) {
		this.message = message;
	}

	public VirtualMachineConfigurationException() {
		this.message = "NO MESSAGE";
	}

	public String getMessage() {
		return message;
	}
}
