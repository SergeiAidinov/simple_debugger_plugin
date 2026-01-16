package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.ConsoleUpdateDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;

public class ConsoleWriter implements Runnable {

	private final InputStream inputStream;
	private final String prefix;

	public ConsoleWriter(InputStream inputStream, String prefix) {
		this.inputStream = inputStream;
		this.prefix = prefix;
	}

	@Override
	public void run() {
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader reader = new BufferedReader(inputStreamReader);
		String line;
		try {
			while (Objects.nonNull(line = reader.readLine())) {
				String text = prefix + line;
				System.out.println(text);
				ConsoleUpdateDebugEvent consoleUpdateDebugEvent = new ConsoleUpdateDebugEvent(
						SimpleDebuggerEventType.REFRESH_CONSOLE, text);
				SimpleDebuggerEventQueue.instance().collectDebugEvent(consoleUpdateDebugEvent);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
