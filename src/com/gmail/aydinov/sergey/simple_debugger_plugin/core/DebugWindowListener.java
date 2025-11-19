package com.gmail.aydinov.sergey.simple_debugger_plugin.core;
/**
 * слушатель событий UI → дебаггер
 */
public interface DebugWindowListener {

    /** Пользователь нажал Resume */
    void onResumeRequested();

    /** Пользователь запросил Step Over */
    void onStepOverRequested();

    /** Пользователь запросил Evaluate */
    void onEvaluateRequested(String expression);
}

