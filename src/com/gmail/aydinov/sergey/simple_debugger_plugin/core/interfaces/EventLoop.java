package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

public interface EventLoop {

    /** Запускает обработку событий (блокирующий вызов) */
    void runLoop();

    /** Запрашивает остановку цикла (не обязательно мгновенную) */
    void stop();
}
