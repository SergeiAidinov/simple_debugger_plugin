package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.EventLoop;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.VmLifeCycleHandler;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;

public class JdiEventLoop implements EventLoop {

    private final VirtualMachine vm;
    private final BreakpointEventHandler breakpointHandler;
    private final VmLifeCycleHandler vmLifeCycleHandler;

    private volatile boolean running = true;

    public JdiEventLoop(VirtualMachine vm,
                        BreakpointEventHandler breakpointHandler,
                        VmLifeCycleHandler vmLifeCycleHandler) {

        this.vm = Objects.requireNonNull(vm);
        this.breakpointHandler = Objects.requireNonNull(breakpointHandler);
        this.vmLifeCycleHandler = Objects.requireNonNull(vmLifeCycleHandler);
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public void runLoop() {
        EventQueue queue = vm.eventQueue();

        while (running) {
            EventSet set = null;

            try {
                set = queue.remove(500); // wait max 500 ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("EventLoop interrupted");
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

            if (!running) break;
            if (set == null) continue;

            try {
                for (Event ev : set) {
                    if (ev instanceof BreakpointEvent bp) {
                        try {
                            breakpointHandler.handle(bp);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        continue;
                    }

                    if (ev instanceof VMDisconnectEvent || ev instanceof VMDeathEvent) {
                        vmLifeCycleHandler.handleVmStopped();
                        running = false;
                        break;
                    }
                }
            } finally {
                try {
                    set.resume();
                } catch (Exception ignore) {}
            }
        }
    }
}
