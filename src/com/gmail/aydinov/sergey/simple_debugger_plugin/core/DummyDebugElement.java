package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.ILaunch;

public class DummyDebugElement implements IDebugElement {
    @Override
    public IDebugTarget getDebugTarget() { return null; }

    @Override
    public ILaunch getLaunch() { return null; }

    @Override
    public String getModelIdentifier() { return "dummy"; }

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		// TODO Auto-generated method stub
		return null;
	}
}

