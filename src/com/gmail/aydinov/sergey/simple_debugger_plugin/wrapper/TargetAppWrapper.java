package com.gmail.aydinov.sergey.simple_debugger_plugin.wrapper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class TargetAppWrapper {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Target class name not provided!");
            return;
        }

        String targetClassName = args[0];
        String targetBin = System.getProperty("target.classpath");

        if (targetBin == null) {
            System.err.println("System property 'target.classpath' not set!");
            return;
        }

        System.out.println("Launching target class: " + targetClassName);
        System.out.println("Target classpath: " + targetBin);

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-cp", targetBin,
                targetClassName
        );

        pb.inheritIO(); // вывод консоли таргет-приложения в консоль wrapper

        try {
            Process process = pb.start();
            System.out.println("Target process started.");
            int exitCode = process.waitFor();
            System.out.println("Target process exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
