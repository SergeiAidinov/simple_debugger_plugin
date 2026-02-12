package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Objects;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.BreakpointSubscriber;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;

/**
 * Listener for Eclipse breakpoints, forwarding events to the registered
 * subscriber.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class BreakePointListener implements IBreakpointListener, BreakpointSubscriberRegistrar {

	private BreakpointSubscriber breakpointSubscriber;

	/**
	 * Called when a breakpoint is added in Eclipse.
	 *
	 * @param breakpoint the breakpoint that was added
	 */
	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		try {
			if (!breakpoint.isEnabled()) {
				breakpoint.setEnabled(true);
			}
		} catch (CoreException exception) {
			exception.printStackTrace();
		}
		breakpointSubscriber.addBreakepoint(breakpoint);
		logBreakpoint(breakpoint, "ADDED");
	}

	/**
	 * Called when a breakpoint is removed in Eclipse.
	 *
	 * @param breakpoint the breakpoint that was removed
	 * @param delta      marker delta (ignored)
	 */
	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		breakpointSubscriber.deleteBreakepoint(breakpoint);
		logBreakpoint(breakpoint, "REMOVED");
	}

	/**
	 * Called when a breakpoint is changed. Currently no action is taken.
	 *
	 * @param breakpoint the breakpoint that changed
	 * @param delta      marker delta (ignored)
	 */
	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		// No action needed for now
	}

	/**
	 * Logs information about a breakpoint.
	 *
	 * @param breakpoint the breakpoint to log
	 * @param action     the action performed (e.g., "ADDED" or "REMOVED")
	 */
	private void logBreakpoint(IBreakpoint breakpoint, String action) {
		try {
			IResource resource = breakpoint.getMarker().getResource();
			String projectName = (Objects.nonNull(resource) && Objects.nonNull(resource.getProject()))
					? resource.getProject().getName()
					: "Unknown Project";
			String fileName = (Objects.nonNull(resource)) ? resource.getName() : "Unknown File";
			int lineNumber = breakpoint.getMarker().getAttribute("lineNumber", -1);
			boolean enabled = breakpoint.isEnabled();

			SimpleDebuggerLogger.info(action + " -> project: " + projectName + ", file: " + fileName + ", line: "
					+ lineNumber + ", enabled: " + enabled);

		} catch (Exception exception) {
			SimpleDebuggerLogger.error(exception.getMessage(), exception);
		}
	}

	/**
	 * Registers a subscriber that will receive breakpoint events.
	 *
	 * @param breakpointSubscriber the subscriber to register
	 */
	@Override
	public void register(BreakpointSubscriber breakpointSubscriber) {
		this.breakpointSubscriber = breakpointSubscriber;
	}
}
