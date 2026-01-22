package com.gmail.aydinov.sergey.simple_debugger_plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DebugConfiguration {
    private final String mainClassName;
    private List<String> vmOptions;
    private int port;

    private final Path workingDirectory;         // root проекта / рабочая директория
    private final Path outputFolder;             // папка с бинарниками (.class)
    private final List<Path> additionalClasspath; // JAR зависимости

    public DebugConfiguration(
            String mainClass,
            List<String> vmOptions,
            int port,
            Path workingDirectory,
            Path outputFolder,
            List<Path> additionalClasspath
    ) {
        this.mainClassName = mainClass;
        this.vmOptions = List.copyOf(vmOptions);
        this.port = port;
        this.workingDirectory = workingDirectory;
        this.outputFolder = outputFolder;
        this.additionalClasspath = List.copyOf(additionalClasspath);
    }

    public String getMainClassName() {
        return mainClassName;
    }

    public List<String> getVmOptions() {
        return vmOptions;
    }

    public int getPort() {
        return port;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public Path getOutputFolder() {
        return outputFolder;
    }

    public List<Path> getAdditionalClasspath() {
        return additionalClasspath;
    }

    public String asVmOptionsString() {
        return String.join(" ", vmOptions);
    }

    // 🔹 собираем classpath: бинарники + JAR
    public String buildClasspathString() {
        List<String> paths = new ArrayList<>();
        paths.add(outputFolder.toString()); // бинарники проекта
        for (Path p : additionalClasspath) {
            paths.add(p.toString());
        }
        return String.join(System.getProperty("path.separator"), paths);
    }

    // 🔹 строка для args.get("options") (-cp + VM options)
    public String buildOptionsString() {
        String cp = buildClasspathString();
        String vmOpts = asVmOptionsString();
        return "-cp \"" + cp + "\" " + vmOpts;
    }
    
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DebugConfiguration ===\n");
        sb.append("Main class: ").append(mainClassName).append("\n");
        sb.append("Port: ").append(port).append("\n");
        sb.append("Working directory: ").append(
            workingDirectory != null ? workingDirectory.toString() : "null"
        ).append("\n");
        sb.append("Output folder: ").append(
            outputFolder != null ? outputFolder.toString() : "null"
        ).append("\n");

        sb.append("Classpath:\n");
        sb.append("  ").append(outputFolder != null ? outputFolder.toString() : "").append("\n");
        for (Path jar : additionalClasspath) {
            sb.append("  ").append(jar.toString()).append("\n");
        }

        sb.append("VM Options:\n");
        for (String opt : vmOptions) {
            sb.append("  ").append(opt).append("\n");
        }

        sb.append("===========================\n");
        return sb.toString();
    }
    
    public String getVmOptionsStringWithoutJDWP() {
        return vmOptions.stream()
                .filter(opt -> !opt.contains("-agentlib:jdwp"))
                .collect(Collectors.joining(" "));
    }
    
    public void setVmOptions(String[] options) {
        this.vmOptions = List.of(options);
    }

    public void setPort(int port) {
        this.port = port;
    }
}
