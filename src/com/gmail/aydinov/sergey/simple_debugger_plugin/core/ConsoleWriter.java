package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.ConsoleUpdateDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;

/**
 * Reads an InputStream (e.g., process output) and sends each line to the debug
 * console.
 */
public class ConsoleWriter implements Runnable {

	private final InputStream inputStream;
	private final String prefix;

	/**
	 * Creates a new ConsoleWriter for a given InputStream.
	 *
	 * @param inputStream the input stream to read from
	 * @param prefix      a string to prepend to each line (e.g., "STDOUT: " or
	 *                    "STDERR: ")
	 */
	public ConsoleWriter(InputStream inputStream, String prefix) {
		this.inputStream = inputStream;
		this.prefix = prefix;
	}

	/**
	 * Continuously reads lines from the input stream and sends them as
	 * ConsoleUpdateDebugEvents to the event queue.
	 */
	@Override
	public void run() {
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		String line;
		try {
			while (Objects.nonNull(line = bufferedReader.readLine())) {
				if (DebuggerContext.context().isInTerminalState()) break;
				String text = prefix + line;
				SimpleDebuggerEventQueue.instance().collectDebugEvent(new ConsoleUpdateDebugEvent(
						SimpleDebuggerEventType.REFRESH_CONSOLE, text));
			}
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
}
