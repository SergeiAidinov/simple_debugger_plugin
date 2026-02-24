package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractSimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.SimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event_collectors.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;

public class DebugWindowsManager implements Runnable {

	private static DebugWindowsManager INSTANCE;

	private DebugWindow debugWindow;
	private InspectWindow inspectWindow;

	/** Минимальный ресурсный источник: карта с изображениями */
	public final Map<String, Image> icons;

	private DebugWindowsManager() {
		Map<String, Image> iconsTemp = new HashMap<>();
		List<PairDTO<String, String>> namesAndPaths = List.of(PairDTO.of("debugger", "/icons/icon.png"),
				PairDTO.of("inspectIcon", "/icons/inspect.png"), PairDTO.of("variableIcon", "/icons/variable.png"),
				PairDTO.of("fieldIcon", "/icons/field.png"));
		namesAndPaths.forEach(p -> iconsTemp.put(p.getKey(), loadIcon(p.getValue())));
		icons = Map.copyOf(iconsTemp);
	}

	public static synchronized DebugWindowsManager instance() {
		if (Objects.isNull(INSTANCE)) {
			INSTANCE = new DebugWindowsManager();
			Thread debugWindowsmanagerThread = new Thread(INSTANCE);
			debugWindowsmanagerThread.setDaemon(true);
			debugWindowsmanagerThread.start();
		}
		return INSTANCE;
	}

	@Override
	public void run() {
		windowsManaging();
	}

	/**
	 * Возвращает или создаёт главное окно
	 */
	public DebugWindow getOrCreateDebugWindow() {
		if (DebuggerContext.context().isInTerminalState())
			return null;

		if (Objects.isNull(debugWindow) || !debugWindow.isOpen()) {
			Display.getDefault().syncExec(() -> {
				debugWindow = new DebugWindow(); // теперь создается в UI-потоке
				debugWindow.open();
			});
		}
		return debugWindow;
	}

	/**
	 * Открывает или обновляет InspectWindow
	 */
	private InspectWindow openNewInspectWindow() {
		if (!DebuggerContext.context().isRunning())
			return null;
		if (Objects.nonNull(inspectWindow) && inspectWindow.isOpen()) {
			inspectWindow.close();
		}
		Display.getDefault().syncExec(() -> {
			inspectWindow = new InspectWindow();
			inspectWindow.open();

		});
		return inspectWindow;
	}

	/**
	 * Загружает изображение один раз и кладёт его в карту
	 */
	public Image loadIcon(String path) {
		Image image = null;
		try (InputStream is = getClass().getResourceAsStream(path)) {
			if (is != null) {
				image = new Image(Display.getDefault(), is);
			} else {
				SimpleDebuggerLogger.warn("Icon not found: " + path);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return image;
	}

	/**
	 * Continuously processes debug events from the global event queue. This method
	 * blocks when no events are available and will only stop if the thread is
	 * interrupted.
	 */
	private void windowsManaging() {
		while (!DebuggerContext.context().isInTerminalState()) {
			try {
				AbstractSimpleDebugEvent event = SimpleDebuggerEventQueue.instance().takeDebugEvent();
				SimpleDebuggerLogger.info("SimpleDebugEvent: " + event);
				
				if (SimpleDebuggerEventTypes.isDebugWindowEvent(event.getType()) && Objects.equals(event.getType(),
						SimpleDebuggerEventTypes.DebugEventType.DISPLAY_INSPECTION_WINDOW)) {
					openNewInspectWindow();
				} else if (Objects.nonNull(debugWindow) && debugWindow.isOpen()) {
					debugWindow.handleDebugEvent(event);
					continue;
				}

				// handling inspection windows events
				if (SimpleDebuggerEventTypes.isInspectionWindowEvent(event.getType())) {
					if (Objects.equals(event.getType(),
							SimpleDebuggerEventTypes.DebugEventType.DISPLAY_INSPECTION_WINDOW))
						openNewInspectWindow();
					else if (Objects.nonNull(inspectWindow) && inspectWindow.isOpen())
						inspectWindow.handleDebugEvent(event);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break; // exit loop if interrupted
			}
		}
	}
}