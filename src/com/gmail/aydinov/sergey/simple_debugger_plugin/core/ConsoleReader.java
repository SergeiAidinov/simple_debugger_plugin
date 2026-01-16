package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class ConsoleReader implements Runnable {
	
	private final InputStream inputStream;
	private final String prefix;
	
	

	public ConsoleReader(InputStream inputStream, String prefix) {
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
				    System.out.println(prefix + line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

}
