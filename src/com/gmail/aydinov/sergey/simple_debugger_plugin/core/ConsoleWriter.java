package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConsoleWriter implements Runnable {

    private final Process process;

    public ConsoleWriter(Process process) {
        this.process = process;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread() + " ConsoleWriter started");

        try (BufferedReader reader =
                     new BufferedReader(
                         new InputStreamReader(
                             process.getInputStream(),
                             StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[TARGET] " + line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("ConsoleWriter finished");
    }
}
