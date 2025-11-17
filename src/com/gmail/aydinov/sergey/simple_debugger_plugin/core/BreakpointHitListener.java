package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.BreakpointWrapper;

public interface BreakpointHitListener {
	void onBreakpointHit(BreakpointWrapper wrapper);
}
