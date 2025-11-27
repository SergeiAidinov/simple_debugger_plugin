package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow;

@FunctionalInterface
public interface OnWorkflowReadyListener {
	 void onReady(SimpleDebuggerWorkFlow workflow);
}
