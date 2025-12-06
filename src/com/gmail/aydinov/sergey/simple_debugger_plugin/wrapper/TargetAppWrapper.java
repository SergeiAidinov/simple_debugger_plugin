package com.gmail.aydinov.sergey.simple_debugger_plugin.wrapper;

public class TargetAppWrapper {

    public static void main(String[] args) {
        System.out.println("TargetAppWrapper started!");
        if (args != null && args.length > 0) {
            System.out.println("Received target class: " + args[0]);
        } else {
            System.out.println("No target class provided.");
        }
    }
}
