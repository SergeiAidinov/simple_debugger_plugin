package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

@FunctionalInterface
public interface OnWorkflowReadyListener {
	 void onReady(SimpleDebuggerWorkFlow workflow);
}
